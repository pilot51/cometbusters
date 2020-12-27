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

object Simulation {
	/** Number of simulations per second. */
	const val TICK_RATE = 100

	/** Indicates if simulation and rendering is paused. */
	private var isPaused = false

	/** Indicates if gameplay has started. */
	var isStarted = false
		set(start) {
			if (start == isStarted) return
			field = start
			listeners.forEach {
				it.onGameStartStateChanged(start)
			}
		}

	/**
	 * @return Time in milliseconds, based on simulation cycles (1000 / [TICK_RATE] ms per cycle),
	 * that the simulation has been running (unpaused).
	 */
	var simulationTime: Long = 0
		private set
	private val listeners = ArrayList<GameStateListener>()
	fun addGameStateListener(listener: GameStateListener) {
		listeners.add(listener)
	}

	fun isPaused(): Boolean {
		return isPaused
	}

	fun setPaused(pause: Boolean) {
		if (pause == isPaused) return
		isPaused = pause
		listeners.forEach {
			it.onGamePauseStateChanged(pause)
		}
	}

	/** Runs one simulation cycle. */
	fun simulate() {
		if (isPaused) return
		val localShip = ShipManager.localShip
		simulationTime += (1000 / TICK_RATE).toLong()
		if (!localShip.isDestroyed) {
			localShip.calculateMotion()
		}
		Asteroid.asteroids.forEach {
			it.calculateMotion()
		}
		if (MultiplayerManager.instance.isClient) {
			localShip.bullets.removeAll { it.isDestroyed }
		} else {
			ShipManager.ships.filterNotNull().forEach { ship ->
				ship.bullets.forEach {
					it.calculateMotion()
				}
			}
			checkCollisions()
		}
		MultiplayerManager.instance.sendShipUpdate()
	}

	private fun checkCollisions() {
		if (MultiplayerManager.instance.isClient) return
		val localShip = ShipManager.localShip
		ShipManager.ships.filterNotNull().forEach { ship ->
			// Ship vs. ship
			if (ship != localShip && localShip.isContacting(ship)) {
				localShip.collide(ship)
			}
			// Ship vs. asteroid
			Asteroid.asteroids.filter { ship.isContacting(it) }.forEach {
				ship.collide(it)
			}
			run bulletsLoop@ { ship.bullets.reversed().forEach { bullet ->
				// Local bullet vs. remote ship
				if (ship == localShip) {
					ShipManager.ships.find { it != null && it != localShip && bullet.isContacting(it) }?.let {
						bullet.collide(it)
					}
				}
				// Remote bullet vs. local ship
				if (ship != localShip && bullet.isContacting(localShip)) {
					bullet.collide(localShip)
					return@bulletsLoop
				}
				// Bullet vs. asteroid
				Asteroid.asteroids.findLast { bullet.isContacting(it) }?.let {
					bullet.collide(it)
					bullet.hitAsteroidSize = it.size
				}
				updateScore(ship, bullet)
				if (bullet.isDestroyed) {
					ship.bullets.remove(bullet)
				}
			}}
		}
	}

	private fun updateScore(ship: Ship, b: Bullet) {
		b.hitAsteroidSize?.run {
			ship.addScore(scoreValue)
			b.hitAsteroidSize = null
		}
	}

	interface GameStateListener {
		fun onGameStartStateChanged(started: Boolean)
		fun onGamePauseStateChanged(paused: Boolean)
	}
}
