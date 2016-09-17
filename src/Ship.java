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

import javax.imageio.ImageIO;


public final class Ship extends Entity {
	private static final int THRUST = 1, ROTATE_SPEED = 1, MAX_BULLETS = 4, SPAWN_TIME_IN_MS = 300; 
	private BufferedImage image, imageSpawning;
	private Image[] thrustImages;
	private int thrustRadius;
	private AffineTransform trans = new AffineTransform();
	private final List<Bullet> BULLETS = new ArrayList<Bullet>(MAX_BULLETS);
	private int score = 0;
	private long birthTime, aliveTime;
	
	/**
	 * Creates a new ship, initially not spawned. Call {@link #spawn(int, int)} to spawn the ship.
	 * @throws IOException if thrust images could not be read.
	 */
	Ship() throws IOException {
		super(0, 0, 0, 0, THRUST, ROTATE_SPEED);
		super.destroy();
		image = ImageIO.read(getClass().getClassLoader().getResource("img/ship.png"));
		radius = image.getWidth(null) / 2;
		thrustImages = new Image[] {ImageIO.read(getClass().getClassLoader().getResource("img/thrust1.png")),
		                            ImageIO.read(getClass().getClassLoader().getResource("img/thrust2.png"))};
		thrustRadius = thrustImages[0].getWidth(null) / 2;
	}
	
	void drawShip(Graphics2D g2d) {
		if (isDestroyed()) return;
		aliveTime = Simulation.getSimulationTime() - birthTime;
		boolean isSpawning = aliveTime < SPAWN_TIME_IN_MS;
		if (isAccelerating && !isSpawning) {
			g2d.drawImage(getThrustImage(), getThrustTransform(), null);
		}
		if (isSpawning) {
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
		boolean isSpawning = aliveTime < SPAWN_TIME_IN_MS;
		double scale = 1, scaledRadius = radius;
		if (isSpawning) {
			scale = (double)aliveTime / SPAWN_TIME_IN_MS;
			scaledRadius = radius * scale;
		}
		trans.setToTranslation(posX - scaledRadius, posY - scaledRadius);
		trans.rotate(Math.toRadians(rotateDeg), scaledRadius, scaledRadius);
		if (isSpawning) {
			trans.scale(scale, scale);
		}
		return trans;
	}
	
	private AffineTransform getThrustTransform() {
		trans.setToTranslation(posX - (thrustRadius - 1), posY + radius / 2);
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
			float bulletX = (float)(posX + Math.sin(Math.toRadians(rotateDeg)) * (radius - Bullet.getBulletRadius())),
			      bulletY = (float)(posY - Math.cos(Math.toRadians(rotateDeg)) * (radius - Bullet.getBulletRadius()));
			BULLETS.add(new Bullet(bulletX, bulletY, rotateDeg));
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
	 * Spawns this ship at the specified coordinates, motionless and pointed up.
	 */
	void spawn(int x, int y) {
		posX = x;
		posY = y;
		rotateDeg = 0;
		velX = 0;
		velY = 0;
		super.undestroy();
		birthTime = Simulation.getSimulationTime();
		Audio.SPAWN.play();
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
		Audio.MUSIC_GAME.stop();
		if (score > 3000) {
			Audio.MUSIC_HIGHSCORE.play();
		} else {
			Audio.MUSIC_DEATH.play();
		}
	}

	int getScore() {
		return score;
	}

	void addScore(int score) {
		this.score += score;
	}

	void resetScore() {
		score = 0;
	}
}
