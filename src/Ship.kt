/*
 * Copyright 2013 Mark Injerd
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

import java.awt.Color
import java.awt.Graphics2D
import java.awt.Image
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Creates a new ship, initially not spawned. Call [spawn] to spawn the ship. */
class Ship internal constructor() : Entity(0f, 0f, 0, 0, THRUST, ROTATE_SPEED) {
	private var image: BufferedImage? = null
	private var imageSpawning: BufferedImage? = null
	private lateinit var thrustImages: Array<Image>
	private var thrustRadius: Int = 0
	private val trans = AffineTransform()

	/**
	 * Gets a list of live bullets fired by this ship, after removing expired bullets.
	 * @return List of live [Bullet]'s fired from this ship.
	 */
	val bullets: MutableList<Bullet> = ArrayList(MAX_BULLETS)
	var score = 0
	var lives = 5

	/** @return The highest number of lives this ship has had. */
	var maxLives = lives
		private set
	private var birthTime: Long = 0
	private var aliveTime: Long = 0

	init {
		super.destroy()
		setPlayerColor(0)
		radius = image!!.getWidth(null) / 2
		try {
			thrustImages = arrayOf(ImageIO.read(javaClass.getResource("thrust1.png")),
					ImageIO.read(javaClass.getResource("thrust2.png")))
			thrustRadius = thrustImages[0].getWidth(null) / 2
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	/** Forces update of ship position, rotation, and thrust. */
	fun forceUpdate(x: Float, y: Float, rotationDeg: Int, thrust: Boolean) {
		pos.x = x
		pos.y = y
		rotateDeg = rotationDeg
		thrust(thrust)
	}

	/**
	 * Updates the ship image with the correct player color.
	 * @param playerId The player ID used to determine color.
	 */
	fun setPlayerColor(playerId: Int) {
		image = RenderUtils.getPlayerShipImage(playerId)
	}

	fun drawShip(g2d: Graphics2D) {
		if (isDestroyed) return
		aliveTime = Simulation.simulationTime - birthTime
		if (isAccelerating && !isSpawning) {
			g2d.drawImage(thrustImage, thrustTransform, null)
		}
		if (isSpawning) {
			if (imageSpawning == null) {
				imageSpawning = RenderUtils.convertImageToSingleColorWithAlpha(image, Color.WHITE)
			}
		} else {
			imageSpawning = null
		}
		g2d.drawImage(if (imageSpawning != null) imageSpawning else image, transform, null)
	}

	private var thrustFrame = 0
	private val thrustImage: Image
		get() {
			if (++thrustFrame == thrustImages.size) {
				thrustFrame = 0
			}
			return thrustImages[thrustFrame]
		}
	private val transform: AffineTransform
		get() {
			var scale = 1.0
			var scaledRadius = radius.toDouble()
			if (isSpawning) {
				scale = spawnProgress()
				scaledRadius = radius * scale
			}
			trans.setToTranslation(pos.x - scaledRadius, pos.y - scaledRadius)
			trans.rotate(Math.toRadians(rotateDeg.toDouble()), scaledRadius, scaledRadius)
			if (isSpawning) {
				trans.scale(scale, scale)
			}
			return trans
		}
	private val thrustTransform: AffineTransform
		get() {
			trans.setToTranslation((pos.x - (thrustRadius - 1)).toDouble(), (pos.y + radius / 2).toDouble())
			trans.rotate(Math.toRadians(rotateDeg.toDouble()), (thrustRadius - 1).toDouble(), (-radius / 2).toDouble())
			return trans
		}

	/**
	 * Makes ship accelerate forward if set to true.<br></br>
	 * Plays or stops thrust sound.
	 * @param activate True to activate thrust, false to deactivate.
	 */
	fun thrust(activate: Boolean) {
		if (isAccelerating == activate) return
		isAccelerating = activate
		if (activate) Audio.THRUST.loop() else Audio.THRUST.stop()
	}

	/**
	 * Fires a [Bullet] in the direction that this ship is facing.<br></br>
	 * Only {@value #MAX_BULLETS} bullets may be live simultaneously, at which point firing is prevented.<br></br>
	 * Also plays shooting sound.
	 */
	fun fire() {
		if (bullets.size < MAX_BULLETS) {
			Audio.SHOOT.play()
			val bulletX = (pos.x + sin(Math.toRadians(rotateDeg.toDouble())) * (radius - Bullet.bulletRadius)).toFloat()
			val bulletY = (pos.y - cos(Math.toRadians(rotateDeg.toDouble())) * (radius - Bullet.bulletRadius)).toFloat()
			val bullet = Bullet(ShipManager.getPlayerId(this), bulletX, bulletY, rotateDeg)
			synchronized(bullets) { bullets.add(bullet) }
			MultiplayerManager.instance.sendFiredBullet(bullet)
		}
	}

	/** @return True if it is relatively safe to respawn, false if an asteroid is too close to the spawn point. */
	private val isSafeHaven: Boolean
		get() {
			synchronized(Asteroid.asteroids) {
				for (a in Asteroid.asteroids) {
					if (abs(pos.x - a.pos.x) + abs(pos.y - a.pos.y) < radius + a.radius + 100) {
						return false
					}
				}
				return true
			}
		}

	/** Spawns this ship at the coordinates defined for the player id, motionless and pointed up. */
	fun spawn() {
		super.undestroy()
		birthTime = Simulation.simulationTime
		lives--
		Audio.SPAWN.play()
	}

	val isSpawning: Boolean
		get() = aliveTime < MATERIALIZE_TIME

	fun spawnProgress(): Double {
		return aliveTime.toDouble() / MATERIALIZE_TIME
	}

	/** Removes this ship from the field. */
	fun terminate() {
		thrust(false)
		rotateStop()
		super.destroy()
	}

	/** Destroys this ship with an explosion. */
	override fun destroy() {
		terminate()
		Audio.EXPLODE_PLAYER.play()
		if (lives > 0) {
			reset(false)
			val deathTime = Simulation.simulationTime
			Timer().schedule(object : TimerTask() {
				override fun run() {
					if (!isDestroyed || !Simulation.isStarted) {
						cancel()
					} else if (Simulation.simulationTime - deathTime >= RESPAWN_DELAY && isSafeHaven) {
						spawn()
						cancel()
					}
				}
			}, Simulation.TICK_RATE.toLong(), (1000 / Simulation.TICK_RATE).toLong())
		} else {
			var gameOver = true
			for (ship in ShipManager.ships) {
				if (ship == null) continue
				if (!ship.isDestroyed || ship.lives > 0) {
					gameOver = false
					break
				}
			}
			if (gameOver) {
				LevelManager.gameOver()
			}
		}
	}

	fun addScore(scoreToAdd: Int) {
		if (score / NEW_SHIP_SCORE < (score + scoreToAdd) / NEW_SHIP_SCORE) {
			lives++
			if (lives > maxLives) {
				maxLives = lives
			}
			Audio.EXTRA_LIFE.play()
		}
		score += scoreToAdd
	}

	/**
	 * Resets position, rotation, velocity, and optionally score and lives.
	 * @param resetForNewGame True to reset lives and score for new game, false to leave them unchanged for respawning.
	 */
	fun reset(resetForNewGame: Boolean) {
		val startPos = ShipManager.getSpawnPosition(ShipManager.getPlayerId(this))
		pos.x = startPos.x
		pos.y = startPos.y
		rotateDeg = 0
		velX = 0f
		velY = 0f
		if (resetForNewGame) {
			score = 0
			lives = 5
		}
	}

	companion object {
		private const val THRUST = 1
		private const val ROTATE_SPEED = 1
		private const val MAX_BULLETS = 4
		private const val RESPAWN_DELAY = 2000
		private const val MATERIALIZE_TIME = 300
		private const val NEW_SHIP_SCORE = 10000
	}
}
