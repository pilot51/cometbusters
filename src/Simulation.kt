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

import java.util.*

object Simulation {
	/** Number of simulations per second.  */
	const val TICK_RATE = 100

	/** Indicates if simulation and rendering is paused.  */
	private var isPaused = false

	/** Indicates if gameplay has started.  */
	var isStarted = false
		set(start) {
			if (start == isStarted) return
			field = start
			for (listener in listeners) {
				listener.onGameStartStateChanged(start)
			}
		}

	/**
	 * @return Time in milliseconds, based on simulation cycles (1000 / [.TICK_RATE] ms per cycle), that the simulation has been running (unpaused).
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
		for (listener in listeners) {
			listener.onGamePauseStateChanged(pause)
		}
	}

	/** Runs one simulation cycle.  */
	fun simulate() {
		if (isPaused) return
		val isClient: Boolean = MultiplayerManager.instance.isClient
		val localShip = ShipManager.localShip
		simulationTime += (1000 / TICK_RATE).toLong()
		if (!localShip.isDestroyed) {
			localShip.calculateMotion()
		}
		synchronized(Asteroid.asteroids) {
			for (a in Asteroid.asteroids) {
				a.calculateMotion()
			}
		}
		if (isClient) {
			synchronized(localShip.bullets) {
				localShip.bullets.removeIf { it.isDestroyed }
			}
		} else {
			for (ship in ShipManager.ships) {
				if (ship == null) continue
				for (b in ship.bullets) {
					b.calculateMotion()
				}
			}
			checkCollisions()
		}
		MultiplayerManager.instance.sendShipUpdate()
	}

	private fun checkCollisions() {
		if (MultiplayerManager.instance.isClient) return
		val localShip = ShipManager.localShip
		for (ship in ShipManager.ships) {
			if (ship == null) continue
			// Ship vs. ship
			if (ship != localShip && localShip.isContacting(ship)) {
				localShip.collide(ship)
			}
			// Ship vs. asteroid
			synchronized(Asteroid.asteroids) {
				for (i in Asteroid.asteroids.indices.reversed()) {
					val a: Asteroid = Asteroid.asteroids[i]
					if (ship.isContacting(a)) {
						ship.collide(a)
					}
				}
			}
			synchronized(ship.bullets) {
				for ((i, bullet) in ship.bullets.withIndex().reversed()) {
					// Local bullet vs. remote ship
					if (ship == localShip) {
						for (remoteShip in ShipManager.ships) {
							if (remoteShip == null || remoteShip == localShip) continue
							if (bullet.isContacting(remoteShip)) {
								bullet.collide(remoteShip)
								break
							}
						}
					}
					// Remote bullet vs. local ship
					if (ship != localShip && bullet.isContacting(localShip)) {
						bullet.collide(localShip)
						break
					}
					// Bullet vs. asteroid
					synchronized(Asteroid.asteroids) {
						for (n in Asteroid.asteroids.indices.reversed()) {
							val a: Asteroid = Asteroid.asteroids[n]
							if (bullet.isContacting(a)) {
								bullet.collide(a)
								bullet.hitAsteroidSize = a.size
								break
							}
						}
					}
					updateScore(ship, bullet)
					if (bullet.isDestroyed) {
						ship.bullets.removeAt(i)
					}
				}
			}
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
