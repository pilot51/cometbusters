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
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;


public final class Ship extends Entity {
	private static final int THRUST = 1, ROTATE_SPEED = 1, MAX_BULLETS = 4; 
	private Image image;
	private Image[] thrustImages;
	private int thrustRadius;
	private AffineTransform trans = new AffineTransform();
	private final List<Bullet> BULLETS = new ArrayList<Bullet>(MAX_BULLETS);
	private Integer score = 0;
	
	/**
	 * Creates a new ship.
	 * @param x Initial x-position.
	 * @param y Initial y-position.
	 * @throws IOException if thrust images could not be read.
	 */
	Ship(int x, int y) throws IOException {
		super(x, y, 0, 0, THRUST, ROTATE_SPEED);
		image = ImageIO.read(getClass().getClassLoader().getResource("img/ship.png"));
		radius = image.getWidth(null) / 2;
		thrustImages = new Image[] {ImageIO.read(getClass().getClassLoader().getResource("img/thrust1.png")),
		                            ImageIO.read(getClass().getClassLoader().getResource("img/thrust2.png"))};
		thrustRadius = thrustImages[0].getWidth(null) / 2;
	}
	
	void drawShip(Graphics2D g2d) {
		if (isDestroyed()) return;
		if (isAccelerating) {
			g2d.drawImage(getThrustImage(), getThrustTransform(), null);
		}
		g2d.drawImage(image, getTransform(), null);
	}
	
	private int thrustFrame;
	private Image getThrustImage() {
		if (++thrustFrame == thrustImages.length) {
			thrustFrame = 0;
		}
		return thrustImages[thrustFrame];
	}
	
	private AffineTransform getTransform() {
		trans.setToTranslation(posX - radius, posY - radius);
		trans.rotate(Math.toRadians(rotateDeg), radius, radius);
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
		if (activate) Sound.THRUST.loop();
		else Sound.THRUST.stop();
	}
	
	/**
	 * Fires a {@link Bullet} in the direction that this ship is facing.<br />
	 * Only {@value #MAX_BULLETS} bullets may be live simultaneously, at which point firing is prevented.<br />
	 * Also plays shooting sound.
	 */
	void fire() {
		if (BULLETS.size() < MAX_BULLETS) {
			Sound.SHOOT.play();
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

	@Override
	void destroy() {
		thrust(false);
		rotateStop();
		Sound.EXPLODE_PLAYER.play();
		super.destroy();
	}

	public Integer getScore() {
		return score;
	}

	public void addScore(Integer score) {
		this.score += score;
	}
}
