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
import Platform.Network.ConnectionManager.Companion.instance as connManager
import Simulation.GameStateListener
import kotlin.math.max
import kotlin.math.min

class MultiplayerManager private constructor() {
	val players: MutableList<RemotePlayer> = ArrayList()
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

	fun onReceive(player: RemotePlayer, msg: String) {
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
						bullets.add(Bullet(
							data[PLAYER_ID].toInt(),
							data[i * SHIP_BULLET_DATA_SIZE + SHIP_BULLET_POSX].toFloat(),
							data[i * SHIP_BULLET_DATA_SIZE + SHIP_BULLET_POSY].toFloat(),
							data[i * SHIP_BULLET_DATA_SIZE + SHIP_BULLET_DIR].toInt()
						))
						i++
					}
				}
				onReceivePlayerData(player, data[PLAYER_ID].toInt(),
					data[POSX].toFloat(), data[POSY].toFloat(), data[DIRECTION].toInt(),
					data[ACCEL].toBoolean(), data[DESTROYED].toBoolean(), data[SCORE].toInt(),
					data[LIVES].toInt(), bullets)
			}
			MESSAGE_TYPE_BULLET -> onReceiveFiredBulletData(player.ship!!, Bullet(
				data[PLAYER_ID].toInt(),
				data[POSX].toFloat(), data[POSY].toFloat(), data[DIRECTION].toInt()
			))
		}
	}

	fun startHost() {
		ShipManager.addShip(ShipManager.localShip, 0)
		connManager.startHost()
	}

	fun connect(address: String?) {
		if (address.isNullOrBlank()) return
		isClient = true
		connManager.connect(address)
	}

	/** Disconnects all players. */
	fun disconnect() {
		if (players.isEmpty()) {
			endMultiplayerSession()
		} else {
			players.reversed().forEach { disconnect(it) }
		}
	}

	/**
	 * Disconnects one player.
	 * @param player The player to disconnect.
	 */
	fun disconnect(player: RemotePlayer) {
		if (!players.remove(player)) return
		connManager.disconnect(player)
		onDisconnected(player)
	}

	private fun sendGameData() {
		if (players.isEmpty() || isClient) return
		val data = arrayOfNulls<String>(3)
		data[MESSAGE_TYPE] = MESSAGE_TYPE_GAME.toString()
		data[IS_STARTED] = Simulation.isStarted.toString()
		data[IS_PAUSED] = Simulation.isPaused().toString()
		send(data)
	}

	fun sendLevel() {
		if (players.isEmpty() || isClient) return
		val data = arrayOfNulls<String>(3)
		data[MESSAGE_TYPE] = MESSAGE_TYPE_LEVEL.toString()
		data[LEVEL] = LevelManager.level.toString()
		send(data)
	}

	/** Sends all data for all asteroids. */
	fun sendAsteroids() {
		if (players.isEmpty() || isClient) return
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
		if (players.isEmpty()) return
		val bullets = ship.bullets.toTypedArray()
		val data = arrayOfNulls<String>(9 + 3 * bullets.size)
		data[MESSAGE_TYPE] = MESSAGE_TYPE_PLAYER.toString()
		data[PLAYER_ID] = ShipManager.ships.indexOf(ship).toString()
		data[POSX] = ship.pos.x.toString()
		data[POSY] = ship.pos.y.toString()
		data[DIRECTION] = ship.rotateDeg.toString()
		data[ACCEL] = ship.isAccelerating.toString()
		data[DESTROYED] = ship.isDestroyed.toString()
		data[SCORE] = ship.score.toString()
		data[LIVES] = ship.lives.toString()
		bullets.forEachIndexed { i, b ->
			data[i * SHIP_BULLET_DATA_SIZE + SHIP_BULLET_POSX] = b.pos.x.toString()
			data[i * SHIP_BULLET_DATA_SIZE + SHIP_BULLET_POSY] = b.pos.y.toString()
			data[i * SHIP_BULLET_DATA_SIZE + SHIP_BULLET_DIR] = b.direction.toString()
		}
		send(data)
	}

	/**
	 * Sends the connection status of a player.
	 * @param player The player whose status is to be sent.
	 * @param connected True if the player is connected, false if not.
	 */
	private fun sendPlayerConnectionStatus(playerId: Int, connected: Boolean) {
		if (players.isEmpty() || isClient) return
		val data = arrayOfNulls<String>(3)
		data[MESSAGE_TYPE] = MESSAGE_TYPE_PLAYER.toString()
		data[PLAYER_ID] = playerId.toString()
		data[PLAYER_IS_CONNECTED] = connected.toString()
		players.filterNot {
			playerId == ShipManager.getPlayerId(it.ship)
		}.forEach { player -> send(player, data) }
	}

	/**
	 * Sends the connection status of all connected players (except the target player) to one client.
	 * @param player The player to receive the connection data.
	 */
	private fun sendConnectedPlayers(player: RemotePlayer) {
		if (players.isEmpty() || isClient) return
		players.filterNot { it === player }.forEach { otherPlayer ->
			val data = arrayOfNulls<String>(3)
			data[MESSAGE_TYPE] = MESSAGE_TYPE_PLAYER.toString()
			data[PLAYER_ID] = ShipManager.getPlayerId(otherPlayer.ship).toString()
			data[PLAYER_IS_CONNECTED] = true.toString()
			send(player, data)
		}
	}

	/**
	 * Client: Sends data for the local ship.
	 * Server: Sends data for all ships.
	 */
	fun sendShipUpdate() {
		if (!Simulation.isStarted || players.isEmpty()) return
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
		if (players.isEmpty()) return
		val data = arrayOfNulls<String>(5)
		data[MESSAGE_TYPE] = MESSAGE_TYPE_BULLET.toString()
		data[PLAYER_ID] = bullet.playerId.toString()
		data[POSX] = bullet.pos.x.toString()
		data[POSY] = bullet.pos.y.toString()
		data[DIRECTION] = bullet.direction.toString()
		players.forEach { player ->
			if (bullet.playerId != ShipManager.getPlayerId(player.ship)) {
				send(player, data)
			}
		}
	}

	/**
	 * Sends data to all connections.
	 * @param data The data to be sent.
	 */
	private fun send(data: Array<String?>) {
		players.forEach { send(it, data) }
	}

	/**
	 * Sends data to one connection.
	 * @param player The player to send the data to.
	 * @param data The data to be sent.
	 */
	private fun send(player: RemotePlayer, data: Array<String?>) {
		connManager.send(player, data.joinToString(" "))
	}

	/**
	 * Called when a connection has been established to do some game setup for the new player.
	 * @param player The new player.
	 */
	fun onConnected(player: RemotePlayer) {
		println("Connected!")
		isConnected = true
		connectionListener.onConnected()
		players.add(player)
		if (isClient) {
			player.ship = Ship().also {
				ShipManager.addShip(it, 0)
			}
			println("Local player ID: ${player.id}")
			ShipManager.addShip(ShipManager.localShip, player.id!!)
		} else {
			var remotePlayerId = ShipManager.getPlayerId(null)
			if (remotePlayerId == -1) {
				remotePlayerId = ShipManager.ships.size
			}
			println("Remote client playerId: $remotePlayerId")
			connManager.send(player, remotePlayerId)
			player.id = remotePlayerId
			sendGameData()
			sendAsteroids()
			player.ship = Ship().also {
				if (Simulation.isStarted) it.lives = 0
				ShipManager.addShip(it, remotePlayerId)
			}
			sendConnectedPlayers(player)
			sendPlayerConnectionStatus(ShipManager.getPlayerId(player.ship), true)
		}
	}

	/**
	 * Called when a player connection has been dropped.
	 * @param player The dropped player.
	 */
	fun onDisconnected(player: RemotePlayer) {
		val playerId = ShipManager.getPlayerId(player.ship)
		sendPlayerConnectionStatus(playerId, false)
		ShipManager.removeShip(playerId)
		if (players.isEmpty()) {
			endMultiplayerSession()
		}
	}

	fun endMultiplayerSession() {
		if (!isClient) connManager.stopHost()
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
		println("Receive - Player $playerId ${if (connected) "connected" else "disconnected"}")
		if (connected) {
			ShipManager.addShip(Ship(), playerId)
		} else {
			ShipManager.removeShip(playerId)
		}
	}

	/**
	 * Called when player game data has been received.
	 * @param player The player that the data has been received from. Only used by the host.
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
		player: RemotePlayer, playerId: Int, posX: Float, posY: Float, rotationDeg: Int, thrust: Boolean,
		isDestroyed: Boolean, score: Int, lives: Int, bullets: List<Bullet>?
	) {
		val ships = ShipManager.ships
		if (!isClient && playerId != 0) {
			player.ship!!.forceUpdate(posX, posY, rotationDeg, thrust)
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
	 * @param ship The ship where the shot originated from. Only used by host.
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

	class RemotePlayer {
		var id: Int? = null
		var ship: Ship? = null
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
	}
}
