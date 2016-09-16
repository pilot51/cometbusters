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
	static boolean isPaused;
	/** Indicates if gameplay has started. */
	static boolean isStarted;
	private static long simulationTime = 0;

	/** Runs one simulation cycle. */
	static void simulate(Ship ship) {
		if (isPaused) return;
		simulationTime += 1000 / TICK_RATE;
		if (!ship.isDestroyed()) {
			ship.calculateMotion();
		}
		for (int i = Asteroid.getAsteroids().size() - 1; i >= 0; i--) {
			Asteroid a = Asteroid.getAsteroids().get(i);
			a.calculateMotion();
			if (ship.isContacting(a)) {
				ship.collide(a);
			}
		}
		for (int i = ship.getBullets().size() - 1; i >= 0; i--) {
			Bullet b = ship.getBullets().get(i);
			b.calculateMotion();
			Asteroid.Size hitAsteroidSize = b.getHitAsteroidSize();
			if (hitAsteroidSize != null) {
				switch (hitAsteroidSize) {
				case LARGE:
					ship.addScore(20);
					break;
				case MEDIUM:
					ship.addScore(50);
					break;
				case SMALL:
					ship.addScore(100);
					break;
				}
				b.setHitAsteroidSize(null);
			}
			if (b.isDestroyed()) {
				synchronized (ship.getBullets()) {
					ship.getBullets().remove(i);
				}
			}
		}
	}

	/**
	 * @return Time in milliseconds, based on simulation cycles (1000 / {@link #TICK_RATE} ms per cycle), that the simulation has been running (unpaused).
	 */
	static long getSimulationTime() {
		return simulationTime;
	}
}
