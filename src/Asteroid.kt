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

import java.awt.Graphics2D
import java.awt.Image
import java.awt.geom.AffineTransform
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO

class Asteroid internal constructor(
	x: Float,
	y: Float,
	direction: Int,
	velocity: Int,
	val size: Size
) : Entity(x, y, direction, velocity) {
	private val trans = AffineTransform()

	enum class Size(val scoreValue: Int) {
		LARGE(20),
		MEDIUM(50),
		SMALL(100);
	}

	private constructor(x: Float, y: Float, direction: Int, velocity: Int) : this(x, y, direction, velocity, Size.LARGE)

	private val transform: AffineTransform
		get() {
			trans.setToTranslation((pos.x - radius).toDouble(), (pos.y - radius).toDouble())
			return trans
		}

	override fun destroy() {
		var newSize: Size? = null
		when (size) {
			Size.LARGE -> {
				Audio.EXPLODE_LARGE.play()
				newSize = Size.MEDIUM
			}
			Size.MEDIUM -> {
				Audio.EXPLODE_MEDIUM.play()
				newSize = Size.SMALL
			}
			Size.SMALL -> Audio.EXPLODE_SMALL.play()
		}
		synchronized(asteroids) {
			if (newSize != null) {
				// Create smaller rocks
				var newDir: Int
				var newVel: Int
				for (x in 1..2) {
					newDir = random.nextInt(360)
					newVel = MIN_SPEED + random.nextInt(1 + MAX_SPEED - MIN_SPEED)
					asteroids.add(Asteroid(pos.x, pos.y, newDir, newVel, newSize))
				}
			}
			asteroids.remove(this)
		}
		super.destroy()
		if (asteroids.isEmpty()) {
			LevelManager.nextLevel()
		}
		MultiplayerManager.instance.sendAsteroids()
	}

	companion object {
		val asteroids: MutableList<Asteroid> = ArrayList()
		private const val MAX_ASTEROIDS = 8
		private const val MIN_SPEED = 2
		private const val MAX_SPEED = 8
		private val image = arrayOfNulls<Image>(3)
		private val random = Random()

		/**
		 * Loads asteroid images for the specified level.
		 * If the specified level is 0 or negative, this does nothing.
		 */
		fun setLevelImages(level: Int) {
			if (level < 1) return
			val lvl = (level - 1) % 8 + 1
			try {
				image[Size.LARGE.ordinal] = ImageIO.read(Asteroid::class.java.getResource("asteroid" + lvl + "_large.png"))
				image[Size.MEDIUM.ordinal] = ImageIO.read(Asteroid::class.java.getResource("asteroid" + lvl + "_medium.png"))
				image[Size.SMALL.ordinal] = ImageIO.read(Asteroid::class.java.getResource("asteroid" + lvl + "_small.png"))
			} catch (e: IOException) {
				e.printStackTrace()
			}
		}

		/**
		 * Randomly spawns several large asteroids at screen edges with randomized speed and direction.
		 * Clears any existing asteroids prior to generation.
		 * If the current level is 0 or negative, asteroids are only cleared.
		 */
		fun generateAsteroids() {
			if (image[0] == null) {
				setLevelImages(1)
			}
			synchronized(asteroids) {
				asteroids.clear()
				if (LevelManager.level > 0) {
					while (asteroids.size < MAX_ASTEROIDS) {
						val spawnTopBottom = random.nextBoolean() // Whether to spawn along the top/bottom instead of left/right edges.
						asteroids.add(Asteroid((if (spawnTopBottom) random.nextInt(GameView.VIEW_WIDTH) else 0).toFloat(),
								(if (spawnTopBottom) 0 else random.nextInt(GameView.VIEW_HEIGHT)).toFloat(),
								random.nextInt(360),
								MIN_SPEED + random.nextInt(1 + MAX_SPEED - MIN_SPEED)))
					}
				}
			}
			MultiplayerManager.instance.sendAsteroids()
		}

		fun drawAsteroids(g2d: Graphics2D) {
			synchronized(asteroids) {
				for (a in asteroids) {
					if (!a.isDestroyed) {
						g2d.drawImage(image[a.size.ordinal], a.transform, null)
					}
				}
			}
		}
	}

	init {
		radius = image[size.ordinal]!!.getWidth(null) / 2
	}
}
