/*
 * Copyright 2016-2023 Mark Injerd
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
import MultiplayerManager.GameData.Companion.getData
import Simulation.GameStateListener
import kotlin.math.max
import kotlin.math.min
import Platform.Network.ConnectionManager.Companion.instance as connMan

class MultiplayerManager private constructor() {
	val players: MutableList<RemotePlayer> = ArrayList()
	var isConnected = false
	var isClient = false
	val isHost get() = !isClient
	private var lastAsteroidsUpdate: Long = -1
	private var lastLocalShipUpdate: Long = -1
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
		when (data[MessageType.dataIndex].toInt()) {
			MessageType.GAME.ordinal -> onReceiveGameData(
				GameData.getIsStarted(data),
				GameData.getIsPaused(data)
			)
			MessageType.LEVEL.ordinal -> onReceiveLevel(
				LevelData.getLevel(data)
			)
			MessageType.ASTEROIDS.ordinal -> onReceiveAsteroids(
				AsteroidData.getAsteroids(data)
			)
			MessageType.PLAYER_CONN.ordinal -> onReceivePlayerConnectionStatus(
				PlayerConnData.getPlayerId(data),
				PlayerConnData.getIsConnected(data)
			)
			MessageType.SCORE_LIVES.ordinal -> onReceiveScoreAndLives(
				ScoreLivesData.getPlayerId(data),
				ScoreLivesData.getScore(data),
				ScoreLivesData.getLives(data)
			)
			MessageType.SHIP.ordinal -> onReceiveShip(
				ShipData.getPlayerId(data),
				ShipData.getPosX(data),
				ShipData.getPosY(data),
				ShipData.getDir(data),
				ShipData.getAccel(data),
				ShipData.getVelX(data),
				ShipData.getVelY(data),
				ShipData.getRotation(data),
				ShipData.getDestroyed(data)
			)
			MessageType.BULLET_FIRE.ordinal -> onReceiveFiredBullet(
				FiredBulletData.getBullet(data)
			)
			MessageType.BULLET_DESTROY.ordinal -> onReceiveDestroyedBullet(
				DestroyedBulletData.getPlayerId(data),
				DestroyedBulletData.getBulletIndex(data)
			)
		}
	}

	fun startHost() {
		ShipManager.addShip(ShipManager.localShip, 0)
		connMan.startHost()
	}

	fun connect(address: String?) {
		if (address.isNullOrBlank()) return
		isClient = true
		connMan.connect(address)
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
		connMan.disconnect(player)
		onDisconnected(player)
	}

	fun tick() {
		// Periodically send all asteroids (if server) and local ship to keep players in sync
		if (isHost && Simulation.simulationTime - lastAsteroidsUpdate > 5000) {
			sendAsteroids()
		}
		if (Simulation.simulationTime - lastLocalShipUpdate > 5000) {
			sendLocalShipUpdate()
		}
	}

	private fun sendGameData() {
		if (isClient || players.isEmpty()) return
		send(getData())
	}

	fun sendLevel() {
		if (isClient || players.isEmpty()) return
		send(LevelData.getData())
	}

	/** Sends all data for all asteroids. */
	fun sendAsteroids() {
		if (isClient || players.isEmpty()) return
		send(AsteroidData.getData())
		lastAsteroidsUpdate = Simulation.simulationTime
	}

	/**
	 * Sends the connection status of a player.
	 * @param playerId The id of the player whose status is to be sent.
	 * @param connected True if the player is connected, false if not.
	 */
	private fun sendPlayerConnectionStatus(playerId: Int, connected: Boolean) {
		if (isClient || players.isEmpty()) return
		val data = PlayerConnData.getData(playerId, connected)
		players.filterNot { playerId == it.id }.forEach { player -> send(player, data) }
	}

	/** Sends connection, ship data, score. and lives for all other players to the specified [player]. */
	private fun sendOtherPlayerData(player: RemotePlayer) {
		if (isClient || players.isEmpty()) return
		ShipManager.ships.filterNotNull().forEach { ship ->
			val shipPlayerId = ShipManager.getPlayerId(ship)
			if (shipPlayerId == player.id) return@forEach
			send(player, PlayerConnData.getData(shipPlayerId, true))
			send(player, ShipData.getData(ship))
			send(player, ScoreLivesData.getData(ship))
		}
	}

	/** Sends score and lives for the specified [ship] to all connections. */
	fun sendScoreData(ship: Ship) {
		if (isClient || players.isEmpty()) return
		send(ScoreLivesData.getData(ship))
	}

	/**
	 * Sends data for the specified [ship] to all connections.
	 * @param isReceived True if the data was received from the player and being forwarded to other players.
	 */
	private fun sendShipData(ship: Ship, isReceived: Boolean = false) {
		if (players.isEmpty()) return
		send(ShipData.getData(ship), ShipManager.getPlayerId(ship).takeIf { isReceived })
	}

	/** Sends ship and score data for the specified [ship] to all connections. */
	fun sendShipWithScoreData(ship: Ship) {
		if (isClient || players.isEmpty()) return
		send(ShipData.getData(ship))
		send(ScoreLivesData.getData(ship))
	}

	/** Sends an update of the local ship to all connections if it is not destroyed. */
	fun sendLocalShipUpdate() {
		if (!Simulation.isStarted || players.isEmpty()) return
		ShipManager.localShip.let {
			if (!it.isDestroyed) {
				sendShipData(it)
				lastLocalShipUpdate = Simulation.simulationTime
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
		val data = FiredBulletData.getData(bullet)
		players.forEach { player ->
			if (bullet.playerId != ShipManager.getPlayerId(player.ship)) {
				send(player, data)
			}
		}
	}

	/**
	 * Sends the [index] of a destroyed bullet along with the originating [playerId].
	 * This is only called by the host which manages object destruction.
	 */
	fun sendDestroyedBullet(playerId: Int, index: Int) {
		if (players.isEmpty()) return
		val data = DestroyedBulletData.getData(playerId, index)
		players.forEach { player ->
			send(player, data)
		}
	}

	/** Sends [data] to all connections, except [excludePlayerId] if provided. */
	private fun send(data: Array<String?>, excludePlayerId: Int? = null) {
		players.forEach {
			if (it.id != excludePlayerId) send(it, data)
		}
	}

	/**
	 * Sends data to one connection.
	 * @param player The player to send the data to.
	 * @param data The data to be sent.
	 */
	private fun send(player: RemotePlayer, data: Array<String?>) {
		connMan.send(player, data.joinToString(" "))
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
			connMan.send(player, remotePlayerId)
			player.id = remotePlayerId
			sendGameData()
			sendAsteroids()
			sendPlayerConnectionStatus(remotePlayerId, true)
			player.ship = Ship().also {
				if (Simulation.isStarted) it.lives = 0
				ShipManager.addShip(it, remotePlayerId)
				sendShipWithScoreData(it)
			}
			sendOtherPlayerData(player)
		}
	}

	/**
	 * Called when a player connection has been dropped.
	 * @param player The dropped player.
	 */
	private fun onDisconnected(player: RemotePlayer) {
		val playerId = ShipManager.getPlayerId(player.ship)
		sendPlayerConnectionStatus(playerId, false)
		ShipManager.removeShip(playerId)
		if (players.isEmpty()) {
			endMultiplayerSession()
		}
	}

	private fun endMultiplayerSession() {
		if (isHost) connMan.stopHost()
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
	 * @param playerId The player ID the data belongs to.
	 * @param score The score for the player.
	 * @param lives The number of lives the player has.
	 */
	private fun onReceiveScoreAndLives(
		playerId: Int, score: Int, lives: Int
	) {
		if (isHost) return
		val ship = ShipManager.ships[playerId]!!
		ship.score = score
		ship.lives = lives
	}

	/**
	 * Called when ship data has been received.
	 * @param playerId The player ID the data belongs to.
	 * @param posX The ship x position.
	 * @param posY The ship y position.
	 * @param rotationDeg The ship rotation in degrees.
	 * @param thrust True if the ship is thrusting, false if not.
	 * @param velX The ship x velocity.
	 * @param velY The ship y velocity.
	 * @param rotation The ship rotation speed/direction.
	 * @param isDestroyed True if the ship is destroyed, false if alive.
	 */
	private fun onReceiveShip(
		playerId: Int, posX: Float, posY: Float,
		rotationDeg: Int, thrust: Boolean,
		velX: Float, velY: Float,
		rotation: Int, isDestroyed: Boolean
	) {
		val ship = ShipManager.ships[playerId]!!
		if (isHost && playerId != 0) {
			ship.forceUpdate(posX, posY, rotationDeg, thrust, velX, velY, rotation)
			sendShipData(ship, true)
		} else if (isClient) {
			ship.forceUpdate(posX, posY, rotationDeg, thrust, velX, velY, rotation)
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
	private fun onReceiveFiredBullet(bullet: Bullet) {
		if (isHost) sendFiredBullet(bullet)
		ShipManager.ships[bullet.playerId]!!.bullets.add(bullet)
		Audio.SHOOT.play()
	}

	private fun onReceiveDestroyedBullet(playerId: Int, bulletIndex: Int) {
		if (isHost) return
		try {
			ShipManager.ships[playerId]!!.bullets.removeAt(bulletIndex)
		} catch (e: IndexOutOfBoundsException) {
			e.printStackTrace()
		}
	}

	/**
	 * Called when asteroid data is received.
	 * @param updatedAsteroids List of asteroids.
	 */
	private fun onReceiveAsteroids(updatedAsteroids: List<Asteroid>?) {
		if (isHost) return
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

	private enum class MessageType {
		GAME,
		LEVEL,
		ASTEROIDS,
		PLAYER_CONN,
		SCORE_LIVES,
		SHIP,
		BULLET_FIRE,
		BULLET_DESTROY;

		companion object {
			/** Index of message type value in data. */
			const val dataIndex = 0
		}
	}

	private enum class GameData {
		IS_STARTED,
		IS_PAUSED;

		/** Index of value in data array, accounting for [MessageType] preceding it. */
		val dataIndex = 1 + ordinal

		companion object {
			val valCount = values().size

			fun getData(): Array<String?> {
				val data = arrayOfNulls<String>(1 + valCount)
				data[MessageType.dataIndex] = MessageType.GAME.ordinal.toString()
				data[IS_STARTED.dataIndex] = Simulation.isStarted.toString()
				data[IS_PAUSED.dataIndex] = Simulation.isPaused().toString()
				return data
			}

			fun getIsStarted(data: Array<String>) = data[IS_STARTED.dataIndex].toBoolean()
			fun getIsPaused(data: Array<String>) = data[IS_PAUSED.dataIndex].toBoolean()
		}
	}

	private enum class LevelData {
		LEVEL;

		/** Index of value in data array, accounting for [MessageType] preceding it. */
		val dataIndex = 1 + ordinal

		companion object {
			val valCount = values().size

			fun getData(): Array<String?> {
				val data = arrayOfNulls<String>(1 + valCount)
				data[MessageType.dataIndex] = MessageType.LEVEL.ordinal.toString()
				data[LEVEL.dataIndex] = LevelManager.level.toString()
				return data
			}

			fun getLevel(data: Array<String>) = data[LEVEL.dataIndex].toInt()
		}
	}

	private enum class AsteroidData {
		POS_X,
		POS_Y,
		DIR,
		VEL,
		SIZE;

		/** Index of value in data array, accounting for [MessageType] preceding it. */
		val dataIndex = 1 + ordinal

		companion object {
			val valCount = values().size

			fun getData(): Array<String?> {
				val data = arrayOfNulls<String>(1 + valCount * Asteroid.asteroids.size)
				data[MessageType.dataIndex] = MessageType.ASTEROIDS.ordinal.toString()
				Asteroid.asteroids.forEachIndexed { i, a ->
					data[i * valCount + POS_X.dataIndex] = a.pos.x.toString()
					data[i * valCount + POS_Y.dataIndex] = a.pos.y.toString()
					data[i * valCount + DIR.dataIndex] = a.direction.toString()
					data[i * valCount + VEL.dataIndex] = a.velocity.toString()
					data[i * valCount + SIZE.dataIndex] = a.size.ordinal.toString()
				}
				return data
			}

			fun getAsteroids(data: Array<String>): List<Asteroid>? {
				var asteroids: MutableList<Asteroid>? = null
				if (data.size > POS_X.dataIndex) {
					asteroids = mutableListOf()
					var i = 0
					while (i * valCount + POS_X.dataIndex < data.size) {
						asteroids.add(
							Asteroid(
								data[i * valCount + POS_X.dataIndex].toFloat(),
								data[i * valCount + POS_Y.dataIndex].toFloat(),
								data[i * valCount + DIR.dataIndex].toInt(),
								data[i * valCount + VEL.dataIndex].toInt(),
								Size.values()[data[i * valCount + SIZE.dataIndex].toInt()]
							)
						)
						i++
					}
				}
				return asteroids
			}
		}
	}

	private enum class PlayerConnData {
		PLAYER_ID,
		IS_CONNECTED;

		/** Index of value in data array, accounting for [MessageType] preceding it. */
		val dataIndex = 1 + ordinal

		companion object {
			val valCount = values().size

			fun getData(playerId: Int, connected: Boolean): Array<String?> {
				val data = arrayOfNulls<String>(1 + valCount)
				data[MessageType.dataIndex] = MessageType.PLAYER_CONN.ordinal.toString()
				data[PLAYER_ID.dataIndex] = playerId.toString()
				data[IS_CONNECTED.dataIndex] = connected.toString()
				return data
			}
			fun getPlayerId(data: Array<String>) = data[PLAYER_ID.dataIndex].toInt()
			fun getIsConnected(data: Array<String>) = data[IS_CONNECTED.dataIndex].toBoolean()
		}
	}

	private enum class ScoreLivesData {
		PLAYER_ID,
		SCORE,
		LIVES;

		/** Index of value in data array, accounting for [MessageType] preceding it. */
		val dataIndex = 1 + ordinal

		companion object {
			val valCount = values().size

			fun getData(ship: Ship): Array<String?> {
				val data = arrayOfNulls<String>(1 + valCount)
				data[MessageType.dataIndex] = MessageType.SCORE_LIVES.ordinal.toString()
				data[PLAYER_ID.dataIndex] = ShipManager.getPlayerId(ship).toString()
				data[SCORE.dataIndex] = ship.score.toString()
				data[LIVES.dataIndex] = ship.lives.toString()
				return data
			}

			fun getPlayerId(data: Array<String>) = data[PLAYER_ID.dataIndex].toInt()
			fun getScore(data: Array<String>) = data[SCORE.dataIndex].toInt()
			fun getLives(data: Array<String>) = data[LIVES.dataIndex].toInt()
		}
	}

	private enum class ShipData {
		PLAYER_ID,
		POS_X,
		POS_Y,
		DIR,
		ACCEL,
		VEL_X,
		VEL_Y,
		ROTATION,
		DESTROYED;

		/** Index of value in data array, accounting for [MessageType] preceding it. */
		val dataIndex = 1 + ordinal

		companion object {
			val valCount = values().size

			fun getData(ship: Ship): Array<String?> {
				val data = arrayOfNulls<String>(1 + valCount)
				data[MessageType.dataIndex] = MessageType.SHIP.ordinal.toString()
				data[PLAYER_ID.dataIndex] = ShipManager.getPlayerId(ship).toString()
				data[POS_X.dataIndex] = ship.pos.x.toString()
				data[POS_Y.dataIndex] = ship.pos.y.toString()
				data[DIR.dataIndex] = ship.rotateDeg.toString()
				data[ACCEL.dataIndex] = ship.isAccelerating.toString()
				data[VEL_X.dataIndex] = ship.velX.toString()
				data[VEL_Y.dataIndex] = ship.velY.toString()
				data[ROTATION.dataIndex] = ship.rotation.toString()
				data[DESTROYED.dataIndex] = ship.isDestroyed.toString()
				return data
			}

			fun getPlayerId(data: Array<String>) = data[PLAYER_ID.dataIndex].toInt()
			fun getPosX(data: Array<String>) = data[POS_X.dataIndex].toFloat()
			fun getPosY(data: Array<String>) = data[POS_Y.dataIndex].toFloat()
			fun getDir(data: Array<String>) = data[DIR.dataIndex].toInt()
			fun getAccel(data: Array<String>) = data[ACCEL.dataIndex].toBoolean()
			fun getVelX(data: Array<String>) = data[VEL_X.dataIndex].toFloat()
			fun getVelY(data: Array<String>) = data[VEL_Y.dataIndex].toFloat()
			fun getRotation(data: Array<String>) = data[ROTATION.dataIndex].toInt()
			fun getDestroyed(data: Array<String>) = data[DESTROYED.dataIndex].toBoolean()
		}
	}

	private enum class FiredBulletData {
		PLAYER_ID,
		POS_X,
		POS_Y,
		DIR;

		/** Index of value in data array, accounting for [MessageType] preceding it. */
		val dataIndex = 1 + ordinal

		companion object {
			val valCount = values().size

			fun getData(bullet: Bullet): Array<String?> {
				val data = arrayOfNulls<String>(1 + valCount)
				data[MessageType.dataIndex] = MessageType.BULLET_FIRE.ordinal.toString()
				data[PLAYER_ID.dataIndex] = bullet.playerId.toString()
				data[POS_X.dataIndex] = bullet.pos.x.toString()
				data[POS_Y.dataIndex] = bullet.pos.y.toString()
				data[DIR.dataIndex] = bullet.direction.toString()
				return data
			}

			fun getBullet(data: Array<String>) = Bullet(
				data[PLAYER_ID.dataIndex].toInt(),
				data[POS_X.dataIndex].toFloat(),
				data[POS_Y.dataIndex].toFloat(),
				data[DIR.dataIndex].toInt()
			)
		}
	}

	private enum class DestroyedBulletData {
		PLAYER_ID,
		BULLET_INDEX;

		/** Index of value in data array, accounting for [MessageType] preceding it. */
		val dataIndex = 1 + ordinal

		companion object {
			val valCount = values().size

			fun getData(playerId: Int, bulletIndex: Int): Array<String?> {
				val data = arrayOfNulls<String>(1 + valCount)
				data[MessageType.dataIndex] = MessageType.BULLET_DESTROY.ordinal.toString()
				data[PLAYER_ID.dataIndex] = playerId.toString()
				data[BULLET_INDEX.dataIndex] = bulletIndex.toString()
				return data
			}

			fun getPlayerId(data: Array<String>) = data[PLAYER_ID.dataIndex].toInt()
			fun getBulletIndex(data: Array<String>) = data[BULLET_INDEX.dataIndex].toInt()
		}
	}

	companion object {
		val instance by lazy { MultiplayerManager() }
	}
}
