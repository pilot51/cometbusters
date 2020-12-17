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

import Asteroid.Size
import java.awt.Graphics2D
import java.awt.Image
import java.awt.geom.AffineTransform
import java.io.IOException
import javax.imageio.ImageIO

/**
 * Creates a new bullet.
 * @param x Initial x-position.
 * @param y Initial y-position.
 * @param deg Bullet direction in degrees.
 */
class Bullet internal constructor(val playerId: Int, x: Float, y: Float, deg: Int) : Entity(x, y, deg, SPEED) {
	/** Size of asteroid that this bullet hit. Null if it has not hit an asteroid. */
	var hitAsteroidSize: Size? = null
	private val timeCreated = Simulation.simulationTime
	private val trans = AffineTransform()

	init {
		super.radius = bulletRadius
	}

	override fun calculateMotion() {
		if (Simulation.simulationTime - timeCreated > DURATION) {
			destroy()
			return
		}
		super.calculateMotion()
	}

	private val transform: AffineTransform
		get() {
			trans.setToTranslation((pos.x - bulletRadius).toDouble(), (pos.y - bulletRadius).toDouble())
			return trans
		}

	companion object {
		private var image: Image? = null

		/** Static method to get bullet radius, since all bullets have the same radius. */
		var bulletRadius = 0
			private set
		private const val DURATION: Long = 2000
		private const val SPEED = 25

		/**
		 * Loads bullet image.
		 * @throws IOException if image could not be read.
		 */
		@Throws(IOException::class)
		fun init() {
			image = ImageIO.read(Bullet::class.java.getResource("deadly_bullet.png")).apply {
				bulletRadius = getWidth(null) / 2
			}
		}

		fun drawBullets(g2d: Graphics2D, ship: Ship) {
			synchronized(ship.bullets) {
				for (b in ship.bullets) {
					if (!b.isDestroyed) {
						g2d.drawImage(image, b.transform, null)
					}
				}
			}
		}
	}
}
