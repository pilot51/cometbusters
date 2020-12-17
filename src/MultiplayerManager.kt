/*
 * Copyright 2016 Mark Injerd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import Asteroid.Size
import Simulation.GameStateListener
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.lang.Runnable
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.UnknownHostException
import java.util.*

class MultiplayerManager private constructor() {
	private val connections: MutableList<Connection> = ArrayList()
	private var serverSocket: ServerSocket? = null
	var isConnected = false
		private set
	var isClient = false
		private set
	private var connectionListener: ConnectionStateListener? = null
	private val gameStateListener: GameStateListener = object : GameStateListener {
		override fun onGameStartStateChanged(started: Boolean) {
			sendGameData()
		}

		override fun onGamePauseStateChanged(paused: Boolean) {
			sendGameData()
		}
	}

	interface ConnectionStateListener {
		fun onHostWaiting()
		fun onConnected()
		fun onDisconnected()
	}

	init {
		Simulation.addGameStateListener(gameStateListener)
	}

	fun setConnectionStateListener(listener: ConnectionStateListener?) {
		connectionListener = listener
	}

	private fun startListening(conn: Connection) {
		Thread {
			while (connections.contains(conn)) {
				val msg = receive(conn)
				if (msg != null) {
					val data = msg.split(" ").toTypedArray()
					when (data[MESSAGE_TYPE].toInt()) {
						MESSAGE_TYPE_GAME -> onReceiveGameData(
							data[IS_STARTED].toBoolean(), data[IS_PAUSED].toBoolean())
						MESSAGE_TYPE_LEVEL -> onReceiveLevel(data[LEVEL].toInt())
						MESSAGE_TYPE_ASTEROIDS -> {
							var asteroids: MutableList<Asteroid>? = null
							if (data.size > ASTEROID_POSX) {
								asteroids = ArrayList()
								var i = 0
								while (i * ASTEROIDS_DATA_SIZE + ASTEROID_POSX < data.size) {
									asteroids.add(Asteroid(data[i * ASTEROIDS_DATA_SIZE + ASTEROID_POSX].toFloat(),
										data[i * ASTEROIDS_DATA_SIZE + ASTEROID_POSY].toFloat(),
										data[i * ASTEROIDS_DATA_SIZE + ASTEROID_DIR].toInt(),
										data[i * ASTEROIDS_DATA_SIZE + ASTEROID_VEL].toInt(),
										Size.valueOf(data[i * ASTEROIDS_DATA_SIZE + ASTEROID_SIZE])))
									i++
								}
							}
							onReceiveAsteroidData(asteroids)
						}
						MESSAGE_TYPE_PLAYER -> if (data.size == 3) {
							onReceivePlayerConnectionStatus(data[PLAYER_ID].toInt(),
								data[PLAYER_IS_CONNECTED].toBoolean())
						} else {
							var bullets: MutableList<Bullet>? = null
							if (data.size > SHIP_BULLET_POSX) {
								bullets = ArrayList()
								var i = 0
								while (i * SHIP_BULLET_DATA_SIZE + SHIP_BULLET_POSX < data.size) {
									bullets.add(Bullet(data[PLAYER_ID].toInt(),
										data[i * SHIP_BULLET_DATA_SIZE + SHIP_BULLET_POSX].toFloat(),
										data[i * SHIP_BULLET_DATA_SIZE + SHIP_BULLET_POSY].toFloat(),
										data[i * SHIP_BULLET_DATA_SIZE + SHIP_BULLET_DIR].toInt()))
									i++
								}
							}
							onReceivePlayerData(conn, data[PLAYER_ID].toInt(),
								data[POSX].toFloat(), data[POSY].toFloat(), data[DIRECTION].toInt(),
								data[ACCEL].toBoolean(), data[DESTROYED].toBoolean(), data[SCORE].toInt(),
								data[LIVES].toInt(), bullets)
						}
						MESSAGE_TYPE_BULLET -> onReceiveFiredBulletData(conn, Bullet(data[PLAYER_ID].toInt(),
							data[POSX].toFloat(), data[POSY].toFloat(), data[DIRECTION].toInt()))
					}
				} else {
					disconnect(conn)
					break
				}
			}
		}.start()
	}

	fun startHost() {
		Thread(Runnable {
			println("Starting host on port $PORT")
			serverSocket = try {
				ServerSocket(PORT)
			} catch (e: IOException) {
				println("Could not listen on port $PORT")
				return@Runnable
			}
			connectionListener!!.onHostWaiting()
			while (serverSocket != null) {
				try {
					val conn = Connection(serverSocket!!.accept())
					connections.add(conn)
					isClient = false
					onConnected(conn)
					startListening(conn)
				} catch (e: IOException) {
					if (serverSocket != null) {
						println("Connection failed!")
						e.printStackTrace()
					}
				}
				if (connections.size == 3) {
					println("Connections full - waiting for a connection to drop before listening for new connections.")
					synchronized(connections) {
						try {
							(connections as java.lang.Object).wait()
						} catch (e: InterruptedException) {
							e.printStackTrace()
						}
					}
					println("Connection dropped - resume listening for new connections.")
				}
			}
		}).start()
	}

	fun connect(address: String?) {
		if (address == null || address.isEmpty()) {
			return
		}
		println("Connecting to server: $address:$PORT")
		Thread {
			try {
				val conn = Connection(address)
				connections.add(conn)
				isClient = true
				onConnected(conn)
				startListening(conn)
			} catch (e: UnknownHostException) {
				println("Unknown host: $address")
			} catch (e: IOException) {
				println("Connection failed!")
				e.printStackTrace()
			}
		}.start()
	}

	/** Disconnects all connections. */
	fun disconnect() {
		for (i in connections.indices.reversed()) {
			disconnect(connections[i])
		}
	}

	/**
	 * Disconnects one connection.
	 * @param conn The connection to disconnect.
	 */
	private fun disconnect(conn: Connection) {
		if (!connections.remove(conn)) return
		synchronized(connections) {
			(connections as java.lang.Object).notify()
		}
		Thread {
			try {
				conn.clientSocket.close()
			} catch (e: IOException) {
				e.printStackTrace()
			}
			println("Disconnected from ${conn.ipRemote.hostAddress}:$PORT" +
				" (playerId ${ShipManager.getPlayerId(conn.remoteShip)})")
			if (connections.isEmpty() && serverSocket != null) {
				try {
					serverSocket!!.close()
					serverSocket = null
					println("Stopped host")
				} catch (e: IOException) {
					e.printStackTrace()
				}
			}
			onDisconnected(conn)
		}.start()
	}

	private fun sendGameData() {
		if (connections.isEmpty() || isClient) return
		val data = arrayOfNulls<String>(3)
		data[MESSAGE_TYPE] = MESSAGE_TYPE_GAME.toString()
		data[IS_STARTED] = Simulation.isStarted.toString()
		data[IS_PAUSED] = Simulation.isPaused().toString()
		send(data)
	}

	fun sendLevel() {
		if (connections.isEmpty() || isClient) return
		val data = arrayOfNulls<String>(3)
		data[MESSAGE_TYPE] = MESSAGE_TYPE_LEVEL.toString()
		data[LEVEL] = LevelManager.level.toString()
		send(data)
	}

	/** Sends all data for all asteroids. */
	fun sendAsteroids() {
		if (connections.isEmpty() || isClient) return
		val data = arrayOfNulls<String>(1 + 5 * Asteroid.asteroids.size)
		data[MESSAGE_TYPE] = MESSAGE_TYPE_ASTEROIDS.toString()
		synchronized(Asteroid.asteroids) {
			for (i in Asteroid.asteroids.indices) {
				val a: Asteroid = Asteroid.asteroids.get(i)
				data[i * ASTEROIDS_DATA_SIZE + ASTEROID_POSX] = a.pos.x.toString()
				data[i * ASTEROIDS_DATA_SIZE + ASTEROID_POSY] = a.pos.y.toString()
				data[i * ASTEROIDS_DATA_SIZE + ASTEROID_DIR] = a.direction.toString()
				data[i * ASTEROIDS_DATA_SIZE + ASTEROID_VEL] = a.velocity.toString()
				data[i * ASTEROIDS_DATA_SIZE + ASTEROID_SIZE] = a.size.name
			}
		}
		send(data)
	}

	/**
	 * Sends data for the specified ship, including its bullets.
	 * @param ship The ship to send.
	 */
	private fun sendShipData(ship: Ship) {
		if (connections.isEmpty()) return
		synchronized(ship.bullets) {
			val data = arrayOfNulls<String>(9 + 3 * ship.bullets.size)
			data[MESSAGE_TYPE] = MESSAGE_TYPE_PLAYER.toString()
			data[PLAYER_ID] = ShipManager.ships.indexOf(ship).toString()
			data[POSX] = ship.pos.x.toString()
			data[POSY] = ship.pos.y.toString()
			data[DIRECTION] = ship.rotateDeg.toString()
			data[ACCEL] = ship.isAccelerating.toString()
			data[DESTROYED] = ship.isDestroyed.toString()
			data[SCORE] = ship.score.toString()
			data[LIVES] = ship.lives.toString()
			for ((i, b) in ship.bullets.withIndex()) {
				data[i * SHIP_BULLET_DATA_SIZE + SHIP_BULLET_POSX] = b.pos.x.toString()
				data[i * SHIP_BULLET_DATA_SIZE + SHIP_BULLET_POSY] = b.pos.y.toString()
				data[i * SHIP_BULLET_DATA_SIZE + SHIP_BULLET_DIR] = b.direction.toString()
			}
			send(data)
		}
	}

	/**
	 * Sends the connection status of a player.
	 * @param playerId The ID of the player to be sent.
	 * @param connected True if the player is connected, false if not.
	 */
	private fun sendPlayerConnectionStatus(playerId: Int, connected: Boolean) {
		if (connections.isEmpty() || isClient) return
		val data = arrayOfNulls<String>(3)
		data[MESSAGE_TYPE] = MESSAGE_TYPE_PLAYER.toString()
		data[PLAYER_ID] = playerId.toString()
		data[PLAYER_IS_CONNECTED] = connected.toString()
		for (conn in connections) {
			if (playerId != ShipManager.getPlayerId(conn.remoteShip)) {
				send(conn, data)
			}
		}
	}

	/**
	 * Sends the connection status of all connected players (minus the target player) to one client.
	 * @param conn The connection to receive the connection data.
	 */
	private fun sendConnectedPlayers(conn: Connection) {
		if (connections.isEmpty() || isClient) return
		for (otherConn in connections) {
			if (otherConn === conn) continue
			val data = arrayOfNulls<String>(3)
			data[MESSAGE_TYPE] = MESSAGE_TYPE_PLAYER.toString()
			data[PLAYER_ID] = ShipManager.getPlayerId(otherConn.remoteShip).toString()
			data[PLAYER_IS_CONNECTED] = true.toString()
			send(conn, data)
		}
	}

	/**
	 * Client: Sends data for the local ship.
	 * Server: Sends data for all ships.
	 */
	fun sendShipUpdate() {
		if (connections.isEmpty()) return
		if (isClient && !ShipManager.localShip.isDestroyed) {
			sendShipData(ShipManager.localShip)
		} else {
			val ships = ShipManager.ships
			synchronized(ships) {
				for (s in ships) {
					if (s == null) continue
					sendShipData(s)
				}
			}
		}
	}

	/**
	 * Sends the initial position and direction of a single bullet along with the ID of the player who shot it.
	 * If host, sends to all clients except the originating client. Clients only send to host.
	 * @param bullet The fired bullet.
	 */
	fun sendFiredBullet(bullet: Bullet) {
		if (connections.isEmpty()) return
		val data = arrayOfNulls<String>(5)
		data[MESSAGE_TYPE] = MESSAGE_TYPE_BULLET.toString()
		data[PLAYER_ID] = bullet.playerId.toString()
		data[POSX] = bullet.pos.x.toString()
		data[POSY] = bullet.pos.y.toString()
		data[DIRECTION] = bullet.direction.toString()
		for (conn in connections) {
			if (bullet.playerId != ShipManager.getPlayerId(conn.remoteShip)) {
				send(conn, data)
			}
		}
	}

	/**
	 * Sends data to all connections.
	 * @param data The data to be sent.
	 */
	private fun send(data: Array<String?>) {
		for (conn in connections) {
			conn.out.println(data.joinToString(" "))
		}
	}

	/**
	 * Sends data to one connection.
	 * @param conn The connection to send the data to.
	 * @param data The data to be sent.
	 */
	private fun send(conn: Connection, data: Array<String?>) {
		conn.out.println(data.joinToString(" "))
	}

	/**
	 * Receives data from a connection.
	 * @param conn The connection to receive the data from.
	 */
	private fun receive(conn: Connection): String? {
		var msg: String? = null
		try {
			msg = conn.`in`.readLine()
		} catch (e: IOException) {
			e.printStackTrace()
		}
		return msg
	}

	/**
	 * Called when a connection has been established to do some game setup for the new player.
	 * @param conn The new connection.
	 */
	private fun onConnected(conn: Connection) {
		println("Connected!")
		isConnected = true
		connectionListener!!.onConnected()
		if (isClient) {
			conn.remoteShip = Ship().also {
				ShipManager.addShip(it, 0)
			}
			val localPlayerId = receive(conn)!!.toInt()
			println("Local player ID: $localPlayerId")
			ShipManager.addShip(ShipManager.localShip, localPlayerId)
		} else {
			var remotePlayerId = ShipManager.getPlayerId(null)
			if (remotePlayerId == -1) {
				remotePlayerId = ShipManager.ships.size
			}
			conn.out.println(remotePlayerId)
			sendGameData()
			sendAsteroids()
			ShipManager.addShip(ShipManager.localShip, 0)
			conn.remoteShip = Ship().also {
				if (Simulation.isStarted) it.lives = 0
				ShipManager.addShip(it, remotePlayerId)
			}
			println("Remote client playerId: $remotePlayerId")
			sendConnectedPlayers(conn)
			sendPlayerConnectionStatus(ShipManager.getPlayerId(conn.remoteShip), true)
		}
	}

	/**
	 * Called when a connection has been dropped.
	 * @param conn The dropped connection.
	 */
	private fun onDisconnected(conn: Connection) {
		val playerId = ShipManager.getPlayerId(conn.remoteShip)
		sendPlayerConnectionStatus(playerId, false)
		ShipManager.removeShip(playerId)
		if (connections.isEmpty()) {
			Simulation.isStarted = false
			isConnected = false
			isClient = false
			connectionListener!!.onDisconnected()
			ShipManager.clearShips()
		}
	}

	private fun onReceiveGameData(isStarted: Boolean, isPaused: Boolean) {
		Simulation.isStarted = isStarted
		Simulation.setPaused(isPaused)
	}

	private fun onReceiveLevel(level: Int) {
		LevelManager.startLevel(level)
	}

	/**
	 * Called when a player connection status has been received.
	 * @param playerId The player ID for the status.
	 * @param connected True if the player is connected, false if not.
	 */
	private fun onReceivePlayerConnectionStatus(playerId: Int, connected: Boolean) {
		println("Receive - Player ${if (connected) "connected" else "disconnected"}: $playerId")
		if (connected) {
			ShipManager.addShip(Ship(), playerId)
		} else {
			ShipManager.removeShip(playerId)
		}
	}

	/**
	 * Called when player game data has been received.
	 * @param conn The connection that the data has been received from. Only used by the host.
	 * @param playerId The player ID the data belongs to.
	 * @param posX The ship x position.
	 * @param posY The ship y position.
	 * @param rotationDeg The ship rotation in degrees.
	 * @param thrust True if the ship is thrusting, false if not.
	 * @param isDestroyed True if the ship is destroyed, false if alive.
	 * @param score The score for the player.
	 * @param lives The number of lives the player has.
	 * @param bullets List of active bullets that the player has shot.
	 */
	private fun onReceivePlayerData(
		conn: Connection, playerId: Int, posX: Float, posY: Float, rotationDeg: Int, thrust: Boolean,
		isDestroyed: Boolean, score: Int, lives: Int, bullets: List<Bullet>?
	) {
		val ships = ShipManager.ships
		if (!isClient && playerId != 0) {
			conn.remoteShip!!.forceUpdate(posX, posY, rotationDeg, thrust)
		} else if (isClient) {
			if (playerId >= ships.size) {
				println("Received data for uninitialized player")
				return
			}
			val ship = ships[playerId]
			if (ship == null) {
				println("Received data for null player")
				return
			}
			ship.score = score
			ship.lives = lives
			if (ship != ShipManager.localShip) {
				ship.forceUpdate(posX, posY, rotationDeg, thrust)
			}
			synchronized(ship.bullets) {
				ship.bullets.clear()
				bullets?.let { ship.bullets.addAll(it) }
			}
			if (isDestroyed && !ship.isDestroyed) {
				ship.destroy()
			} else if (!isDestroyed && ship.isDestroyed) {
				ship.spawn()
			}
		}
	}

	/**
	 * Called when data for a fired bullet is received.
	 * If host, it is forwarded to other clients and added to the player. If client, only the shoot sound is played.
	 * @param conn The connection where the shot originated from, only used by host.
	 * @param bullet The fired bullet.
	 */
	private fun onReceiveFiredBulletData(conn: Connection, bullet: Bullet) {
		if (!isClient) {
			sendFiredBullet(bullet)
			conn.remoteShip!!.bullets.let {
				synchronized(it) { it.add(bullet) }
			}
		}
		Audio.SHOOT.play()
	}

	/**
	 * Called when asteroid data is received.
	 * @param updatedAsteroids List of asteroids.
	 */
	private fun onReceiveAsteroidData(updatedAsteroids: List<Asteroid>?) {
		val asteroids = Asteroid.asteroids
		synchronized(asteroids) {
			if (updatedAsteroids == null) {
				if (asteroids.size == 1 && asteroids[0].size == Size.SMALL) {
					Audio.EXPLODE_SMALL.play()
				}
				asteroids.clear()
			} else {
				for (i in Math.min(asteroids.size, updatedAsteroids.size)
						until Math.max(asteroids.size, updatedAsteroids.size)) {
					if (i >= asteroids.size) {
						when (updatedAsteroids[i].size) {
							Size.LARGE -> {}
							Size.MEDIUM -> Audio.EXPLODE_LARGE.play()
							Size.SMALL -> Audio.EXPLODE_MEDIUM.play()
						}
					} else if (i >= updatedAsteroids.size) {
						Audio.EXPLODE_SMALL.play()
					}
				}
				asteroids.clear()
				asteroids.addAll(updatedAsteroids)
			}
		}
	}

	private inner class Connection {
		val clientSocket: Socket
		val out: PrintWriter
		val `in`: BufferedReader
		val ipRemote: InetAddress
		var remoteShip: Ship? = null

		/** Host constructor */
		constructor(clientSocket: Socket) {
			this.clientSocket = clientSocket
			out = PrintWriter(clientSocket.getOutputStream(), true)
			`in` = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
			ipRemote = clientSocket.inetAddress
			println("Incoming connection: ${ipRemote.hostAddress}:${clientSocket.port}")
		}

		/** Client constructor */
		constructor(hostAddress: String) {
			ipRemote = InetAddress.getByName(hostAddress)
			clientSocket = Socket(ipRemote, PORT)
			out = PrintWriter(clientSocket.getOutputStream(), true)
			`in` = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
			println("Local client port: ${clientSocket.localPort}")
		}
	}

	companion object {
		val instance by lazy { MultiplayerManager() }
		private const val MESSAGE_TYPE_GAME = 0
		private const val MESSAGE_TYPE_LEVEL = 1
		private const val MESSAGE_TYPE_ASTEROIDS = 2
		private const val MESSAGE_TYPE_PLAYER = 3
		private const val MESSAGE_TYPE_BULLET = 4
		private const val MESSAGE_TYPE = 0
		private const val IS_STARTED = 1
		private const val IS_PAUSED = 2
		private const val LEVEL = 1
		private const val PLAYER_IS_CONNECTED = 2
		private const val ASTEROIDS_DATA_SIZE = 5
		private const val ASTEROID_POSX = 1
		private const val ASTEROID_POSY = 2
		private const val ASTEROID_DIR = 3
		private const val ASTEROID_VEL = 4
		private const val ASTEROID_SIZE = 5
		private const val PLAYER_ID = 1
		private const val POSX = 2
		private const val POSY = 3
		private const val DIRECTION = 4
		private const val ACCEL = 5
		private const val DESTROYED = 6
		private const val SCORE = 7
		private const val LIVES = 8
		private const val SHIP_BULLET_DATA_SIZE = 3
		private const val SHIP_BULLET_POSX = 9
		private const val SHIP_BULLET_POSY = 10
		private const val SHIP_BULLET_DIR = 11
		private const val PORT = 50001
	}
}
