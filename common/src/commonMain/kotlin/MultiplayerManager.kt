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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class MultiplayerManager private constructor() {
	val connections: MutableList<PlayerConnection> = ArrayList()
	var isConnected = false
	var isClient = false
	lateinit var connectionListener: ConnectionStateListener
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

	fun setConnectionStateListener(listener: ConnectionStateListener) {
		connectionListener = listener
	}

	fun startListening(conn: PlayerConnection) {
		CoroutineScope(Dispatchers.Default).launch {
			while (connections.contains(conn)) {
				val msg = Platform.Network.ConnectionManager.instance.receive(conn)
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
									asteroids.add(
										Asteroid(
											data[i * ASTEROIDS_DATA_SIZE + ASTEROID_POSX].toFloat(),
											data[i * ASTEROIDS_DATA_SIZE + ASTEROID_POSY].toFloat(),
											data[i * ASTEROIDS_DATA_SIZE + ASTEROID_DIR].toInt(),
											data[i * ASTEROIDS_DATA_SIZE + ASTEROID_VEL].toInt(),
											Size.valueOf(data[i * ASTEROIDS_DATA_SIZE + ASTEROID_SIZE])
										)
									)
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
									bullets.add(
										Bullet(
											data[PLAYER_ID].toInt(),
											data[i * SHIP_BULLET_DATA_SIZE + SHIP_BULLET_POSX].toFloat(),
											data[i * SHIP_BULLET_DATA_SIZE + SHIP_BULLET_POSY].toFloat(),
											data[i * SHIP_BULLET_DATA_SIZE + SHIP_BULLET_DIR].toInt()
										)
									)
									i++
								}
							}
							onReceivePlayerData(conn, data[PLAYER_ID].toInt(),
								data[POSX].toFloat(), data[POSY].toFloat(), data[DIRECTION].toInt(),
								data[ACCEL].toBoolean(), data[DESTROYED].toBoolean(), data[SCORE].toInt(),
								data[LIVES].toInt(), bullets)
						}
						MESSAGE_TYPE_BULLET -> onReceiveFiredBulletData(conn.remoteShip!!, Bullet(
							data[PLAYER_ID].toInt(),
							data[POSX].toFloat(), data[POSY].toFloat(), data[DIRECTION].toInt()
						))
					}
				} else {
					disconnect(conn)
					break
				}
			}
		}
	}

	fun startHost() {
		println("Starting host on port $PORT")
		Platform.Network.ConnectionManager.instance.startHost()
	}

	fun connect(address: String?) {
		if (address.isNullOrBlank()) return
		println("Connecting to server: $address:$PORT")
		isClient = true
		Platform.Network.ConnectionManager.instance.connect(address)
	}

	/** Disconnects all connections. */
	fun disconnect() {
		if (connections.isEmpty()) {
			endMultiplayerSession()
		} else {
			connections.reversed().forEach { disconnect(it) }
		}
	}

	/**
	 * Disconnects one connection.
	 * @param conn The connection to disconnect.
	 */
	private fun disconnect(conn: PlayerConnection) {
		if (!connections.remove(conn)) return
		Platform.Network.ConnectionManager.instance.disconnect(conn)
		onDisconnected(conn)
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
		Asteroid.asteroids.forEachIndexed { i, a ->
			data[i * ASTEROIDS_DATA_SIZE + ASTEROID_POSX] = a.pos.x.toString()
			data[i * ASTEROIDS_DATA_SIZE + ASTEROID_POSY] = a.pos.y.toString()
			data[i * ASTEROIDS_DATA_SIZE + ASTEROID_DIR] = a.direction.toString()
			data[i * ASTEROIDS_DATA_SIZE + ASTEROID_VEL] = a.velocity.toString()
			data[i * ASTEROIDS_DATA_SIZE + ASTEROID_SIZE] = a.size.name
		}
		send(data)
	}

	/**
	 * Sends data for the specified ship, including its bullets.
	 * @param ship The ship to send.
	 */
	private fun sendShipData(ship: Ship) {
		if (connections.isEmpty()) return
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
		ship.bullets.toList().forEachIndexed { i, b ->
			data[i * SHIP_BULLET_DATA_SIZE + SHIP_BULLET_POSX] = b.pos.x.toString()
			data[i * SHIP_BULLET_DATA_SIZE + SHIP_BULLET_POSY] = b.pos.y.toString()
			data[i * SHIP_BULLET_DATA_SIZE + SHIP_BULLET_DIR] = b.direction.toString()
		}
		send(data)
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
		connections.filterNot {
			it.playerId == playerId
		}.forEach { conn -> send(conn, data) }
	}

	/**
	 * Sends the connection status of all connected players (minus the target player) to one client.
	 * @param conn The connection to receive the connection data.
	 */
	private fun sendConnectedPlayers(conn: PlayerConnection) {
		if (connections.isEmpty() || isClient) return
		connections.filterNot { it === conn }.forEach { otherConn ->
			val data = arrayOfNulls<String>(3)
			data[MESSAGE_TYPE] = MESSAGE_TYPE_PLAYER.toString()
			data[PLAYER_ID] = otherConn.playerId!!.toString()
			data[PLAYER_IS_CONNECTED] = true.toString()
			send(conn, data)
		}
	}

	/**
	 * Client: Sends data for the local ship.
	 * Server: Sends data for all ships.
	 */
	fun sendShipUpdate() {
		if (!Simulation.isStarted || connections.isEmpty()) return
		if (isClient && !ShipManager.localShip.isDestroyed) {
			sendShipData(ShipManager.localShip)
		} else {
			ShipManager.ships.filterNotNull().forEach {
				sendShipData(it)
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
		connections.forEach { conn ->
			if (bullet.playerId != conn.playerId) {
				send(conn, data)
			}
		}
	}

	/**
	 * Sends data to all connections.
	 * @param data The data to be sent.
	 */
	private fun send(data: Array<String?>) {
		connections.filterNot { it.playerId == null }.forEach {
			Platform.Network.ConnectionManager.instance.send(it, data.joinToString(" "))
		}
	}

	/**
	 * Sends data to one connection.
	 * @param conn The connection to send the data to.
	 * @param data The data to be sent.
	 */
	private fun send(conn: PlayerConnection, data: Array<String?>) {
		connections.filterNot { it.playerId == null }.forEach {
			Platform.Network.ConnectionManager.instance.send(conn, data.joinToString(" "))
		}
	}

	/**
	 * Called when a connection has been established to do some game setup for the new player.
	 * @param conn The new connection.
	 */
	fun onConnected(conn: PlayerConnection) {
		println("Connected!")
		isConnected = true
		connectionListener.onConnected()
		connections.add(conn)
		if (isClient) {
			conn.remoteShip = Ship().also {
				ShipManager.addShip(it, 0)
			}
			val localPlayerId = Platform.Network.ConnectionManager.instance.receive(conn)!!.toInt()
			println("Local player ID: $localPlayerId")
			ShipManager.addShip(ShipManager.localShip, localPlayerId)
		} else {
			var remotePlayerId = ShipManager.getPlayerId(null)
			if (remotePlayerId == -1) {
				remotePlayerId = ShipManager.ships.size
			}
			println("Remote client playerId: $remotePlayerId")
			Platform.Network.ConnectionManager.instance.send(conn, remotePlayerId)
			conn.playerId = remotePlayerId
			sendGameData()
			sendAsteroids()
			ShipManager.addShip(ShipManager.localShip, 0)
			conn.remoteShip = Ship().also {
				if (Simulation.isStarted) it.lives = 0
				ShipManager.addShip(it, remotePlayerId)
			}
			sendConnectedPlayers(conn)
			sendPlayerConnectionStatus(remotePlayerId, true)
		}
		startListening(conn)
	}

	/**
	 * Called when a connection has been dropped.
	 * @param conn The dropped connection.
	 */
	fun onDisconnected(conn: PlayerConnection) {
		val playerId = conn.playerId!!
		println("Disconnected from ${conn.remoteAddress}:${conn.remotePort} (playerId ${playerId})")
		sendPlayerConnectionStatus(playerId, false)
		ShipManager.removeShip(playerId)
		if (connections.isEmpty()) {
			endMultiplayerSession()
		}
	}

	fun endMultiplayerSession() {
		if (!isClient) Platform.Network.ConnectionManager.instance.stopHost()
		Simulation.isStarted = false
		isConnected = false
		isClient = false
		connectionListener.onDisconnected()
		ShipManager.clearShips()
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
		conn: PlayerConnection, playerId: Int, posX: Float, posY: Float, rotationDeg: Int, thrust: Boolean,
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
			ship.bullets.clear()
			bullets?.let { ship.bullets.addAll(it) }
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
	private fun onReceiveFiredBulletData(ship: Ship, bullet: Bullet) {
		if (!isClient) {
			sendFiredBullet(bullet)
			ship.bullets.let {
				it.add(bullet)
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
		if (updatedAsteroids == null) {
			if (asteroids.size == 1 && asteroids[0].size == Size.SMALL) {
				Audio.EXPLODE_SMALL.play()
			}
			asteroids.clear()
		} else {
			for (i in min(asteroids.size, updatedAsteroids.size)
				until max(asteroids.size, updatedAsteroids.size)) {
				if (i >= asteroids.size) {
					when (updatedAsteroids[i].size) {
						Size.LARGE -> {
						}
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

	class PlayerConnection {
		val localPort: Int
		val remoteAddress: String
		val remotePort: Int
		var playerId: Int? = null
		var remoteShip: Ship? = null

		constructor(remoteAddress: String, remotePort: Int, localPort: Int) {
			if (MultiplayerManager.instance.isClient) {
				this.localPort = localPort
				this.remoteAddress = remoteAddress
				this.remotePort = remotePort
				println("Local client port: ${localPort}")
			} else {
				this.localPort = localPort
				this.remoteAddress = remoteAddress
				this.remotePort = remotePort
				println("Incoming connection: ${remoteAddress}:${remotePort}")
			}
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
		const val PORT = 50001
	}
}
