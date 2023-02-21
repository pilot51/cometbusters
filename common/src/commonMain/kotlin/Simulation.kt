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

import MultiplayerManager.Companion.instance as mpMan

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
		simulationTime += (1000 / TICK_RATE).toLong()
		val ships = ShipManager.ships.filterNotNull()
		ships.forEach { ship ->
			if (!ship.isDestroyed) ship.calculateMotion()
			val playerId by lazy { ShipManager.getPlayerId(ship) }
			val iterator = ship.bullets.listIterator()
			while (iterator.hasNext()) {
				val index = iterator.nextIndex()
				val bullet = iterator.next()
				bullet.calculateMotion()
				if (bullet.isDestroyed && mpMan.isHost) {
					mpMan.sendDestroyedBullet(playerId, index)
					iterator.remove()
				}
			}
		}
		Asteroid.asteroids.forEach {
			it.calculateMotion()
		}
		if (mpMan.isHost) checkCollisions()
		mpMan.tick()
	}

	private fun checkCollisions() {
		if (mpMan.isClient) return
		val localShip = ShipManager.localShip
		val ships = ShipManager.ships.filterNotNull()
		ships.forEach { ship ->
			val playerId by lazy { ShipManager.getPlayerId(ship) }
			// Ship vs. ship
			ships.forEach { otherShip ->
				if (ship != otherShip && otherShip.isContacting(ship)) {
					otherShip.collide(ship)
				}
			}
			if (ship != localShip && localShip.isContacting(ship)) {
				localShip.collide(ship)
			}
			// Ship vs. asteroid
			Asteroid.asteroids.filter { ship.isContacting(it) }.forEach {
				ship.collide(it)
			}
			run bulletsLoop@ { ship.bullets.reversed().forEach { bullet ->
				// Bullet vs. other ship
				ships.forEach { otherShip ->
					if (ship != otherShip && bullet.isContacting(otherShip)) {
						bullet.collide(otherShip)
						return@bulletsLoop
					}
				}
				// Bullet vs. asteroid
				Asteroid.asteroids.findLast { bullet.isContacting(it) }?.let {
					bullet.collide(it)
					bullet.hitAsteroidSize = it.size
				}
				updateScore(ship, bullet)
				if (bullet.isDestroyed) {
					val index = ship.bullets.indexOf(bullet)
					ship.bullets.remove(bullet)
					mpMan.sendDestroyedBullet(playerId, index)
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
