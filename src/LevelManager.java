import java.util.Timer;
import java.util.TimerTask;

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

public class LevelManager {
	/** Milliseconds to wait before new level begins. */
	private static final int NEW_LEVEL_WAIT = 3000;
	/** Milliseconds to wait before GAME OVER appears. */
	private static final int BEFORE_GAMEOVER_WAIT = 3000;
	/** Milliseconds to wait on GAME OVER sign. */
	private static final int GAMEOVER_WAIT = 6000;
	/** The current level, or -1 for game over. */
	private static int currentLevel = 1;
	/** Indicates whether the new level or game over text should be shown. */
	private static boolean shouldShowText;

	/** Starts a new game at level 1. */
	static void startGame() {
		Simulation.setStarted(true);
		if (MultiplayerManager.getInstance().isClient()) {
			ShipManager.getLocalShip().reset(true);
		} else {
			startLevel(1);
			for (Ship ship : ShipManager.getShips()) {
				ship.reset(true);
				ship.spawn();
			}
		}
		Audio.MUSIC_GAME.loop();
	}

	/** Stops the game by terminating the ship(s), stopping music if playing, and generating background asteroids. */
	static void stopGame() {
		Simulation.setStarted(false);
		for (Ship ship : ShipManager.getShips()) {
			ship.terminate();
		}
		LevelManager.createBackgroundAsteroids();
		Audio.MUSIC_GAME.stop();
	}

	/** @return The current level, or -1 for game over. */
	static int getLevel() {
		return currentLevel;
	}

	/** @return True if the new level or game over text should be shown. */
	static boolean shouldShowText() {
		return shouldShowText;
	}

	/** @return True if we're in the wait period at the beginning of a level prior to generating asteroids. */
	static boolean isWaitingToStartLevel() {
		return shouldShowText && currentLevel > 0;
	}

	/** @return True if the ship(s) have lost all lives. */
	static boolean isGameOver() {
		return currentLevel == -1;
	}

	/**
	 * Starts the specified level after a delay to allow the level text to be displayed.
	 * @param level The level to start.
	 */
	static void startLevel(int level) {
		currentLevel = level;
		shouldShowText = true;
		if (level == 1) {
			synchronized (Asteroid.getAsteroids()) {
				Asteroid.getAsteroids().clear();
			}
		}
		MultiplayerManager.getInstance().sendLevel();
		Asteroid.setLevelImages(level);
		long startTime = Simulation.getSimulationTime();
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				if (!Simulation.isStarted() || !Asteroid.getAsteroids().isEmpty()) {
					cancel();
					shouldShowText = false;
				} else if (Simulation.getSimulationTime() - startTime >= NEW_LEVEL_WAIT) {
					if (!MultiplayerManager.getInstance().isClient()) {
						Asteroid.generateAsteroids();
					}
					cancel();
					shouldShowText = false;
				}
			}
		}, Simulation.TICK_RATE, 1000 / Simulation.TICK_RATE);
	}

	/** Starts the next level. */
	static void nextLevel() {
		startLevel(currentLevel + 1);
	}

	/**
	 * Initiates the 'game over' game state. Music changes to end-game music
	 * and the 'game over' text is requested to be shown for {@value #GAMEOVER_WAIT} ms
	 * after a {@value #BEFORE_GAMEOVER_WAIT} ms delay.
	 */
	static void gameOver() {
		currentLevel = -1;
		Audio.MUSIC_GAME.stop();
		if (ShipManager.getLocalShip().getScore() > 5000) {
			Audio.MUSIC_HIGHSCORE.play();
		} else {
			Audio.MUSIC_DEATH.play();
		}
		long deathTime = Simulation.getSimulationTime();
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				long timeSinceDeath = Simulation.getSimulationTime() - deathTime;
				if (timeSinceDeath >= BEFORE_GAMEOVER_WAIT + GAMEOVER_WAIT) {
					cancel();
					shouldShowText = false;
				} else if (timeSinceDeath >= BEFORE_GAMEOVER_WAIT) {
					shouldShowText = true;
					Simulation.setStarted(false);
				} else if (!Simulation.isStarted()) {
					cancel();
				}
			}
		}, Simulation.TICK_RATE, 1000 / Simulation.TICK_RATE);
	}

	/** Generates level 1 asteroids to be shown in the background outside of gameplay. */
	static void createBackgroundAsteroids() {
		currentLevel = 1;
		Asteroid.generateAsteroids();
	}
}
