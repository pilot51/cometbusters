import java.util.ArrayList;

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

public class Simulation {
	/** Number of simulations per second. */
	static final int TICK_RATE = 100;
	/** Indicates if simulation and rendering is paused. */
	private static boolean isPaused;
	/** Indicates if gameplay has started. */
	private static boolean isStarted;
	private static long simulationTime = 0;
	private static final ArrayList<GameStateListener> listeners = new ArrayList<GameStateListener>();

	interface GameStateListener {
		void onGameStartStateChanged(boolean started);
		void onGamePauseStateChanged(boolean paused);
	}

	static void addGameStateListener(GameStateListener listener) {
		Simulation.listeners.add(listener);
	}

	static boolean isStarted() {
		return isStarted;
	}

	static boolean isPaused() {
		return isPaused;
	}

	static void setStarted(boolean start) {
		if (start == isStarted) return;
		isStarted = start;
		for (GameStateListener listener : listeners) {
			listener.onGameStartStateChanged(start);
		}
	}

	static void setPaused(boolean pause) {
		if (pause == isPaused) return;
		isPaused = pause;
		for (GameStateListener listener : listeners) {
			listener.onGamePauseStateChanged(pause);
		}
	}

	/** Runs one simulation cycle. */
	static void simulate() {
		if (isPaused) return;
		boolean isClient = MultiplayerManager.getInstance().isClient();
		Ship localShip = ShipManager.getLocalShip();
		simulationTime += 1000 / TICK_RATE;
		if (!localShip.isDestroyed()) {
			localShip.calculateMotion();
		}
		synchronized (Asteroid.getAsteroids()) {
			for (Asteroid a : Asteroid.getAsteroids()) {
				a.calculateMotion();
			}
		}
		if (isClient) {
			synchronized (localShip.getBullets()) {
				for (int i = localShip.getBullets().size() - 1; i >= 0; i--) {
					if (localShip.getBullets().get(i).isDestroyed()) {
						localShip.getBullets().remove(i);
					}
				}
			}
		} else {
			for (Ship ship : ShipManager.getShips()) {
				for (Bullet b : ship.getBullets()) {
					b.calculateMotion();
				}
			}
			checkCollisions();
		}
		MultiplayerManager.getInstance().sendShipUpdate();
	}

	private static void checkCollisions() {
		if (MultiplayerManager.getInstance().isClient()) return;
		Ship localShip = ShipManager.getLocalShip();
		for (Ship ship : ShipManager.getShips()) {
			// Ship vs. ship
			if (ship != localShip && localShip.isContacting(ship)) {
				localShip.collide(ship);
			}
			// Ship vs. asteroid
			synchronized (Asteroid.getAsteroids()) {
				for (int i = Asteroid.getAsteroids().size() - 1; i >= 0; i--) {
					Asteroid a = Asteroid.getAsteroids().get(i);
					if (ship.isContacting(a)) {
						ship.collide(a);
					}
				}
			}
			synchronized (ship.getBullets()) {
				for (int i = ship.getBullets().size() - 1; i >= 0; i--) {
					Bullet bullet = ship.getBullets().get(i);
					// Local bullet vs. remote ship
					if (ship == localShip) {
						for (Ship remoteShip : ShipManager.getShips()) {
							if (remoteShip == localShip) continue;
							if (bullet.isContacting(remoteShip)) {
								bullet.collide(remoteShip);
								break;
							}
						}
					}
					// Remote bullet vs. local ship
					if (ship != localShip && bullet.isContacting(localShip)) {
						bullet.collide(localShip);
						break;
					}
					// Bullet vs. asteroid
					synchronized (Asteroid.getAsteroids()) {
						for (int n = Asteroid.getAsteroids().size() - 1; n >= 0; n--) {
							Asteroid a = Asteroid.getAsteroids().get(n);
							if (bullet.isContacting(a)) {
								bullet.collide(a);
								bullet.setHitAsteroidSize(a.getSize());
								break;
							}
						}
					}
					updateScore(ship, bullet);
					if (bullet.isDestroyed()) {
						ship.getBullets().remove(i);
					}
				}
			}
		}
	}

	private static void updateScore(Ship ship, Bullet b) {
		Asteroid.Size hitAsteroidSize = b.getHitAsteroidSize();
		if (hitAsteroidSize != null) {
			ship.addScore(hitAsteroidSize.getScoreValue());
			b.setHitAsteroidSize(null);
		}
	}

	/**
	 * @return Time in milliseconds, based on simulation cycles (1000 / {@link #TICK_RATE} ms per cycle), that the simulation has been running (unpaused).
	 */
	static long getSimulationTime() {
		return simulationTime;
	}
}
