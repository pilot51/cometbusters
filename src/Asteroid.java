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

public class Asteroid extends Entity {
	private static final List<Asteroid> ASTEROIDS = new ArrayList<Asteroid>();
	private static final int MAX_ASTEROIDS = 8, MIN_SPEED = 2, MAX_SPEED = 8;
	private static final byte SIZE_LARGE = 0, SIZE_MEDIUM = 1, SIZE_SMALL = 2;
	private static Image[] image = new Image[3];
	private static final Random random = new Random();
	private byte size;
	private int radius;
	
	private Asteroid(float x, float y, int rotationDeg, int velocity) {
		super(x, y, rotationDeg, velocity);
		size = SIZE_LARGE;
		radius = image[size].getWidth(null) / 2;
	}

	/**
	 * Loads asteroid images for provided level and randomly spawns several
	 * large asteroids at screen edges with randomized speed and direction.
	 * @param level The level for which to generate asteroids.
	 * @throws IOException If asteroid images could not be read.
	 */
	static void generateAsteroids(int level) throws IOException {
		image[SIZE_LARGE] = ImageIO.read(Asteroid.class.getClassLoader().getResource("img/asteroid" + level + "_large.png"));
		image[SIZE_MEDIUM] = ImageIO.read(Asteroid.class.getClassLoader().getResource("img/asteroid" + level + "_medium.png"));
		image[SIZE_SMALL] = ImageIO.read(Asteroid.class.getClassLoader().getResource("img/asteroid" + level + "_small.png"));
		while (ASTEROIDS.size() < MAX_ASTEROIDS) {
			boolean spawnTopBottom = random.nextBoolean(); // Whether to spawn along the top/bottom instead of left/right edges.
			ASTEROIDS.add(new Asteroid(spawnTopBottom ? random.nextInt(GameView.VIEW_WIDTH) : 0,
			                           spawnTopBottom ? 0 : random.nextInt(GameView.VIEW_HEIGHT),
			                           random.nextInt(360),
			                           MIN_SPEED + random.nextInt(1 + MAX_SPEED - MIN_SPEED)));
		}
	}
	
	private AffineTransform trans = new AffineTransform();
	private AffineTransform getTransform() {
		trans.setToTranslation(posX - radius, posY - radius);
		return trans;
	}
	
	static void drawAsteroids(Graphics2D g2d) {
		for (Asteroid a : ASTEROIDS) {
			a.calculateMotion();
			g2d.drawImage(image[a.size], a.getTransform(), null);
		}
	}
}
