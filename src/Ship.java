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

import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;


public class Ship extends Entity {
	private static final int THRUST = 1, ROTATE_SPEED = 1, MAX_BULLETS = 4; 
	private Image image;
	private Image[] thrustImages;
	private int shipRadius, thrustRadius;
	private AffineTransform trans = new AffineTransform();
	private final List<Bullet> BULLETS = new ArrayList<Bullet>(MAX_BULLETS);
	
	/**
	 * Creates a new ship.
	 * @param img Image to use for the ship.
	 * @param x Initial x-position.
	 * @param y Initial y-position.
	 * @throws IOException if thrust images could not be read.
	 */
	Ship(Image img, int x, int y) throws IOException {
		super(x, y, THRUST, ROTATE_SPEED);
		image = img;
		shipRadius = image.getWidth(null) / 2;
		thrustImages = new Image[] {ImageIO.read(getClass().getClassLoader().getResource("img/thrust1.png")),
		                            ImageIO.read(getClass().getClassLoader().getResource("img/thrust2.png"))};
		thrustRadius = thrustImages[0].getWidth(null) / 2;
	}
	
	Image getImage() {
		return image;
	}
	
	private int thrustFrame;
	Image getThrustImage() {
		if (++thrustFrame == thrustImages.length) {
			thrustFrame = 0;
		}
		return thrustImages[thrustFrame];
	}
	
	AffineTransform getTransform() {
		trans.setToTranslation(posX - shipRadius, posY - shipRadius);
		trans.rotate(Math.toRadians(rotateDeg), shipRadius, shipRadius);
		return trans;
	}
	
	AffineTransform getThrustTransform() {
		trans.setToTranslation(posX - (thrustRadius - 1), posY + shipRadius / 2);
		trans.rotate(Math.toRadians(rotateDeg), thrustRadius - 1, -shipRadius / 2);
		return trans;
	}
	
	boolean isThrustActive() {
		return isAccelerating;
	}
	
	void thrust(boolean activate) {
		if (isAccelerating == activate) return;
		isAccelerating = activate;
		if (activate) Sound.THRUST.loop();
		else Sound.THRUST.stop();
	}
	
	void fire() {
		if (BULLETS.size() < MAX_BULLETS) {
			Sound.SHOOT.play();
			float bulletX = (float)(posX + Math.sin(Math.toRadians(rotateDeg)) * (shipRadius - Bullet.getRadius())),
			      bulletY = (float)(posY - Math.cos(Math.toRadians(rotateDeg)) * (shipRadius - Bullet.getRadius()));
			BULLETS.add(new Bullet(bulletX, bulletY, rotateDeg));
		}
	}
	
	List<Bullet> getBullets() {
		for (int i = BULLETS.size() - 1; i >= 0; i--) {
			if (BULLETS.get(i).isExpired()) {
				BULLETS.remove(i);
			}
		}
		return BULLETS;
	}
}
