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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;


public final class Ship extends Entity {
	private static final int THRUST = 1, ROTATE_SPEED = 1, MAX_BULLETS = 4, RESPAWN_DELAY = 2000, MATERIALIZE_TIME = 300, NEW_SHIP_SCORE = 10000;
	private BufferedImage image, imageSpawning;
	private Image[] thrustImages;
	private int thrustRadius;
	private final AffineTransform trans = new AffineTransform();
	private final List<Bullet> BULLETS = new ArrayList<Bullet>(MAX_BULLETS);
	private int score = 0, lives = 5, maxLives = lives;
	private long birthTime, aliveTime;
	
	/**
	 * Creates a new ship, initially not spawned. Call {@link #spawn(int, int)} to spawn the ship.
	 */
	Ship() {
		super(0, 0, 0, 0, THRUST, ROTATE_SPEED);
		super.destroy();
		setPlayerColor(0);
		radius = image.getWidth(null) / 2;
		try {
			thrustImages = new Image[] {ImageIO.read(getClass().getClassLoader().getResource("img/thrust1.png")),
			                            ImageIO.read(getClass().getClassLoader().getResource("img/thrust2.png"))};
		} catch (IOException e) {
			e.printStackTrace();
		}
		thrustRadius = thrustImages[0].getWidth(null) / 2;
	}
	
	/**
	 * Forces update of ship position, rotation, and thrust.
	 */
	void forceUpdate(float x, float y, int rotationDeg, boolean thrust) {
		pos.x = x;
		pos.y = y;
		rotateDeg = rotationDeg;
		thrust(thrust);
	}

	/**
	 * Updates the ship image with the correct player color.
	 * @param playerId The player ID used to determine color.
	 */
	void setPlayerColor(int playerId) {
		image = RenderUtils.getPlayerShipImage(playerId);
	}

	void drawShip(Graphics2D g2d) {
		if (isDestroyed()) return;
		aliveTime = Simulation.getSimulationTime() - birthTime;
		if (isAccelerating && !isSpawning()) {
			g2d.drawImage(getThrustImage(), getThrustTransform(), null);
		}
		if (isSpawning()) {
			if (imageSpawning == null) {
				imageSpawning = RenderUtils.convertImageToSingleColorWithAlpha(image, Color.WHITE);
			}
		} else {
			imageSpawning = null;
		}
		g2d.drawImage(imageSpawning != null ? imageSpawning : image, getTransform(), null);
	}

	private int thrustFrame;
	private Image getThrustImage() {
		if (++thrustFrame == thrustImages.length) {
			thrustFrame = 0;
		}
		return thrustImages[thrustFrame];
	}
	
	private AffineTransform getTransform() {
		double scale = 1, scaledRadius = radius;
		if (isSpawning()) {
			scale = spawnProgress();
			scaledRadius = radius * scale;
		}
		trans.setToTranslation(pos.x - scaledRadius, pos.y - scaledRadius);
		trans.rotate(Math.toRadians(rotateDeg), scaledRadius, scaledRadius);
		if (isSpawning()) {
			trans.scale(scale, scale);
		}
		return trans;
	}
	
	private AffineTransform getThrustTransform() {
		trans.setToTranslation(pos.x - (thrustRadius - 1), pos.y + radius / 2);
		trans.rotate(Math.toRadians(rotateDeg), thrustRadius - 1, -radius / 2);
		return trans;
	}
	
	/**
	 * Makes ship accelerate forward if set to true.<br />
	 * Plays or stops thrust sound.
	 * @param activate True to activate thrust, false to deactivate.
	 */
	void thrust(boolean activate) {
		if (isAccelerating == activate) return;
		isAccelerating = activate;
		if (activate) Audio.THRUST.loop();
		else Audio.THRUST.stop();
	}
	
	/**
	 * Fires a {@link Bullet} in the direction that this ship is facing.<br />
	 * Only {@value #MAX_BULLETS} bullets may be live simultaneously, at which point firing is prevented.<br />
	 * Also plays shooting sound.
	 */
	void fire() {
		if (BULLETS.size() < MAX_BULLETS) {
			Audio.SHOOT.play();
			float bulletX = (float)(pos.x + Math.sin(Math.toRadians(rotateDeg)) * (radius - Bullet.getBulletRadius())),
			      bulletY = (float)(pos.y - Math.cos(Math.toRadians(rotateDeg)) * (radius - Bullet.getBulletRadius()));
			Bullet bullet = new Bullet(ShipManager.getPlayerId(this), bulletX, bulletY, rotateDeg);
			synchronized (BULLETS) {
				BULLETS.add(bullet);
			}
			MultiplayerManager.getInstance().sendFiredBullet(bullet);
		}
	}
	
	/**
	 * Gets a list of live bullets fired by this ship, after removing expired bullets. 
	 * @return List of live {@link Bullet}'s fired from this ship.
	 */
	List<Bullet> getBullets() {
		return BULLETS;
	}

	/**
	 * @return True if it is relatively safe to respawn, false if an asteroid is too close to the spawn point.
	 */
	private boolean isSafeHaven() {
		synchronized (Asteroid.getAsteroids()) {
			for (Asteroid a : Asteroid.getAsteroids()) {
				if (Math.abs(pos.x - a.pos.x) + Math.abs(pos.y - a.pos.y) < radius + a.radius + 100) {
					return false;
				}
			}
			return true;
		}
	}

	/**
	 * Spawns this ship at the coordinates defined for the player id, motionless and pointed up.
	 */
	void spawn() {
		super.undestroy();
		birthTime = Simulation.getSimulationTime();
		lives--;
		Audio.SPAWN.play();
	}

	boolean isSpawning() {
		return aliveTime < MATERIALIZE_TIME;
	}

	double spawnProgress() {
		return (double)aliveTime / MATERIALIZE_TIME;
	}

	/**
	 * Removes this ship from the field.
	 */
	void terminate() {
		thrust(false);
		rotateStop();
		super.destroy();
	}

	/**
	 * Destroys this ship with an explosion.
	 */
	@Override
	void destroy() {
		terminate();
		Audio.EXPLODE_PLAYER.play();
		if (lives > 0) {
			reset(false);
			long deathTime = Simulation.getSimulationTime();
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					if (!isDestroyed() || !Simulation.isStarted()) {
						cancel();
					} else if (Simulation.getSimulationTime() - deathTime >= RESPAWN_DELAY && isSafeHaven()) {
						spawn();
						cancel();
					}
				}
			}, Simulation.TICK_RATE, 1000 / Simulation.TICK_RATE);
		} else {
			boolean gameOver = true;
			for (Ship ship : ShipManager.getShips()) {
				if (ship == null) continue;
				if (!ship.isDestroyed() || ship.getLives() > 0) {
					gameOver = false;
					break;
				}
			}
			if (gameOver) {
				LevelManager.gameOver();
			}
		}
	}

	int getLives() {
		return lives;
	}

	void setLives(int lives) {
		this.lives = lives;
	}

	/** @return The highest number of lives this ship has had. */
	int getMaxLives() {
		return maxLives;
	}

	int getScore() {
		return score;
	}

	void setScore(int score) {
		this.score = score;
	}

	void addScore(int scoreToAdd) {
		if (score / NEW_SHIP_SCORE < (score + scoreToAdd) / NEW_SHIP_SCORE) {
			lives++;
			if (lives > maxLives) {
				maxLives = lives;
			}
			Audio.EXTRA_LIFE.play();
		}
		score += scoreToAdd;
	}

	/**
	 * Resets position, rotation, velocity, and optionally score and lives.
	 * @param resetForNewGame True to reset lives and score for new game, false to leave them unchanged for respawning.
	 */
	void reset(boolean resetForNewGame) {
		Entity.Position startPos = ShipManager.getSpawnPosition(ShipManager.getPlayerId(this));
		pos.x = startPos.x;
		pos.y = startPos.y;
		rotateDeg = 0;
		velX = 0;
		velY = 0;
		if (resetForNewGame) {
			score = 0;
			lives = 5;
		}
	}
}
