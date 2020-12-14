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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.io.IOException;

import javax.imageio.ImageIO;

public final class Bullet extends Entity {
	private static Image image;
	private static int radius;
	/** Size of asteroid that this bullet hit. Null if it has not hit an asteroid. */
	private Asteroid.Size hitAsteroidSize;
	private static final long DURATION = 2000;
	private static final int SPEED = 25;
	private int playerId;
	private long timeCreated;
	private AffineTransform trans = new AffineTransform();
	
	/**
	 * Creates a new bullet.
	 * @param x Initial x-position.
	 * @param y Initial y-position.
	 * @param deg Bullet direction in degrees.
	 */
	Bullet(int playerId, float x, float y, int deg) {
		super(x, y, deg, SPEED);
		super.radius = radius;
		this.playerId = playerId;
		timeCreated = Simulation.getSimulationTime();
	}
	
	/**
	 * Loads bullet image.
	 * @throws IOException if image could not be read.
	 */
	static void init() throws IOException {
		image = ImageIO.read(Bullet.class.getResource("deadly_bullet.png"));
		radius = image.getWidth(null) / 2;
	}
	
	void calculateMotion() {
		if (Simulation.getSimulationTime() - timeCreated > DURATION) {
			destroy();
			return;
		}
		super.calculateMotion();
	}

	static void drawBullets(Graphics2D g2d, Ship ship) {
		synchronized (ship.getBullets()) {
			for (Bullet b : ship.getBullets()) {
				if (!b.isDestroyed()) {
					g2d.drawImage(image, b.getTransform(), null);
				}
			}
		}
	}
	
	/**
	 * Static method to get bullet radius, since all bullets have the same radius.
	 */
	static int getBulletRadius() {
		return radius;
	}
	
	private AffineTransform getTransform() {
		trans.setToTranslation(pos.x - radius, pos.y - radius);
		return trans;
	}

	int getPlayerId() {
		return playerId;
	}

	/**
	 * @return Size of asteroid that this bullet hit, or null if it has not hit an asteroid.
	 */
	public Asteroid.Size getHitAsteroidSize() {
		return hitAsteroidSize;
	}

	public void setHitAsteroidSize(Asteroid.Size hitAsteroidSize) {
		this.hitAsteroidSize = hitAsteroidSize;
	}
}
