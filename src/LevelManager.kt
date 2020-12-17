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

object LevelManager {
	/** Milliseconds to wait before new level begins. */
	private const val NEW_LEVEL_WAIT = 3000

	/** Milliseconds to wait before GAME OVER appears. */
	private const val BEFORE_GAMEOVER_WAIT = 3000

	/** Milliseconds to wait on GAME OVER sign. */
	private const val GAMEOVER_WAIT = 6000
	/** The current level, or -1 for game over. */
	var level = 1
		private set

	/** Indicates whether the new level or game over text should be shown. */
	private var shouldShowText = false

	/** Starts a new game at level 1. */
	fun startGame() {
		Simulation.isStarted = true
		if (MultiplayerManager.instance.isClient) {
			ShipManager.localShip.reset(true)
		} else {
			startLevel(1)
			for (ship in ShipManager.ships) {
				if (ship == null) continue
				ship.reset(true)
				ship.spawn()
			}
		}
		Audio.MUSIC_GAME.loop()
	}

	/** Stops the game by terminating the ship(s), stopping music if playing, and generating background asteroids. */
	fun stopGame() {
		Simulation.isStarted = false
		for (ship in ShipManager.ships) {
			if (ship == null) continue
			ship.terminate()
		}
		createBackgroundAsteroids()
		Audio.MUSIC_GAME.stop()
	}

	/** @return True if the new level or game over text should be shown. */
	fun shouldShowText(): Boolean {
		return shouldShowText
	}

	/** @return True if we're in the wait period at the beginning of a level prior to generating asteroids. */
	val isWaitingToStartLevel: Boolean
		get() = shouldShowText && level > 0

	/** @return True if the ship(s) have lost all lives. */
	val isGameOver: Boolean
		get() = level == -1

	/**
	 * Starts the specified level after a delay to allow the level text to be displayed.
	 * @param level The level to start.
	 */
	fun startLevel(level: Int) {
		LevelManager.level = level
		shouldShowText = true
		if (level == 1) {
			synchronized(Asteroid.asteroids) { Asteroid.asteroids.clear() }
		}
		MultiplayerManager.instance.sendLevel()
		Asteroid.setLevelImages(level)
		val startTime = Simulation.simulationTime
		Timer().schedule(object : TimerTask() {
			override fun run() {
				if (!Simulation.isStarted || !Asteroid.asteroids.isEmpty()) {
					cancel()
					shouldShowText = false
				} else if (Simulation.simulationTime - startTime >= NEW_LEVEL_WAIT) {
					if (!MultiplayerManager.instance.isClient) {
						Asteroid.generateAsteroids()
					}
					cancel()
					shouldShowText = false
				}
			}
		}, Simulation.TICK_RATE.toLong(), (1000 / Simulation.TICK_RATE).toLong())
	}

	/** Starts the next level. */
	fun nextLevel() {
		startLevel(level + 1)
	}

	/**
	 * Initiates the 'game over' game state. Music changes to end-game music
	 * and the 'game over' text is requested to be shown for [GAMEOVER_WAIT] ms
	 * after a [BEFORE_GAMEOVER_WAIT] ms delay.
	 */
	fun gameOver() {
		level = -1
		Audio.MUSIC_GAME.stop()
		if (ShipManager.localShip.score > 5000) {
			Audio.MUSIC_HIGHSCORE.play()
		} else {
			Audio.MUSIC_DEATH.play()
		}
		val deathTime = Simulation.simulationTime
		Timer().schedule(object : TimerTask() {
			override fun run() {
				val timeSinceDeath = Simulation.simulationTime - deathTime
				if (timeSinceDeath >= BEFORE_GAMEOVER_WAIT + GAMEOVER_WAIT) {
					cancel()
					shouldShowText = false
				} else if (timeSinceDeath >= BEFORE_GAMEOVER_WAIT) {
					shouldShowText = true
					Simulation.isStarted = false
				} else if (!Simulation.isStarted) {
					cancel()
				}
			}
		}, Simulation.TICK_RATE.toLong(), (1000 / Simulation.TICK_RATE).toLong())
	}

	/** Generates level 1 asteroids to be shown in the background outside of gameplay. */
	fun createBackgroundAsteroids() {
		level = 1
		Asteroid.generateAsteroids()
	}
}
