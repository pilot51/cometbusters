import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

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

public final class Asteroid extends Entity {
	public enum Size { LARGE, MEDIUM, SMALL }
	private static final List<Asteroid> ASTEROIDS = new ArrayList<Asteroid>();
	private static final int MAX_ASTEROIDS = 8, MIN_SPEED = 2, MAX_SPEED = 8;
	private static Image[] image = new Image[3];
	private static final Random random = new Random();
	private Size size;
	private AffineTransform trans = new AffineTransform();
	
	private Asteroid(float x, float y, int rotationDeg, int velocity) {
		this(x, y, rotationDeg, velocity, Size.LARGE);
	}
	
	private Asteroid(float x, float y, int rotationDeg, int velocity, Size size) {
		super(x, y, rotationDeg, velocity);
		this.size = size;
		radius = image[size.ordinal()].getWidth(null) / 2;
	}

	/**
	 * Loads asteroid images for provided level and randomly spawns several
	 * large asteroids at screen edges with randomized speed and direction.
	 * @param level The level for which to generate asteroids.
	 * @throws IOException If asteroid images could not be read.
	 */
	static void generateAsteroids(int level) throws IOException {
		image[Size.LARGE.ordinal()] = ImageIO.read(Asteroid.class.getClassLoader().getResource("img/asteroid" + level + "_large.png"));
		image[Size.MEDIUM.ordinal()] = ImageIO.read(Asteroid.class.getClassLoader().getResource("img/asteroid" + level + "_medium.png"));
		image[Size.SMALL.ordinal()] = ImageIO.read(Asteroid.class.getClassLoader().getResource("img/asteroid" + level + "_small.png"));
		while (ASTEROIDS.size() < MAX_ASTEROIDS) {
			boolean spawnTopBottom = random.nextBoolean(); // Whether to spawn along the top/bottom instead of left/right edges.
			ASTEROIDS.add(new Asteroid(spawnTopBottom ? random.nextInt(GameView.VIEW_WIDTH) : 0,
			                           spawnTopBottom ? 0 : random.nextInt(GameView.VIEW_HEIGHT),
			                           random.nextInt(360),
			                           MIN_SPEED + random.nextInt(1 + MAX_SPEED - MIN_SPEED)));
		}
	}
	
	private AffineTransform getTransform() {
		trans.setToTranslation(posX - radius, posY - radius);
		return trans;
	}
	
	static void drawAsteroids(Graphics2D g2d) {
		synchronized (ASTEROIDS) {
			for (Asteroid a : ASTEROIDS) {
				if (!a.isDestroyed()) {
					g2d.drawImage(image[a.size.ordinal()], a.getTransform(), null);
				}
			}
		}
	}

	@Override
	void destroy() {
		Size newSize = null;
		switch (size) {
		case LARGE:
			Sound.EXPLODE_LARGE.play();
			newSize = Size.MEDIUM;
			break;
		case MEDIUM:
			Sound.EXPLODE_MEDIUM.play();
			newSize = Size.SMALL;
			break;
		case SMALL:
			Sound.EXPLODE_SMALL.play();
			break;
		}
		synchronized (ASTEROIDS) {
			if (newSize != null) {
				// Create smaller rocks
				int newDir, newVel;
				for (int x = 1; x <= 2; x++) {
					newDir = random.nextInt(360);
					newVel = MIN_SPEED + random.nextInt(1 + MAX_SPEED - MIN_SPEED);
					ASTEROIDS.add(new Asteroid(posX, posY, newDir, newVel, newSize));
				}
			}
			ASTEROIDS.remove(this);
		}
		super.destroy();
	}

	static List<Asteroid> getAsteroids() {
		return ASTEROIDS;
	}
	
	public Size getSize() {
		return size;
	}
}
