
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
