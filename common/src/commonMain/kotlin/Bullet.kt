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
import Platform.Renderer.RenderView2D
import Platform.Renderer.Transform2D
import Platform.Resources.Image

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
	private val trans = Transform2D()

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

	private val transform: Transform2D
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

		/** Loads bullet image. */
		fun init() {
			image = Image("img/deadly_bullet.png") {
				bulletRadius = it.width / 2
			}
		}

		fun drawBullets(view2D: RenderView2D, ship: Ship) {
			ship.bullets.filter { !it.isDestroyed }.forEach {
				view2D.drawImage(image!!, it.transform)
			}
		}
	}
}
