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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

public class RenderUtils {
	static final Color[] PLAYER_COLORS = new Color[] { Color.CYAN, Color.MAGENTA, Color.GREEN, Color.YELLOW };
	private static final int[] PLAYER_HUES = new int[] { 180, 300, 120, 60 };
	static final Color TEXT_LEVEL_COLOR = new Color(255, 32, 128), TEXT_GAMEOVER_COLOR = new Color(32, 255, 64);
	private static BufferedImage baseShipImage;

	/**
	 * Converts all pixels from the source image that aren't fully transparent to the specified color.
	 * @param src Source image to convert.
	 * @param fillColor The desired color.
	 * @return The converted image as a new {@link BufferedImage}.
	 */
	static BufferedImage convertImageToSingleColorWithAlpha(BufferedImage src, Color fillColor) {
		BufferedImage newImage = new BufferedImage(src.getWidth(null), src.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < src.getWidth(); x++) {
			for (int y = 0; y < src.getHeight(); y++) {
				if (new Color(src.getRGB(x, y), true).getAlpha() == 255) {
					newImage.setRGB(x, y, fillColor.getRGB());
				}
			}
		}
		return newImage;
	}

	static void drawLives(Graphics2D g2d, int lives, Color color, int x, int y) {
		Ship ship = ShipManager.getLocalShip();
		BufferedImage imageLife = RenderUtils.convertImageToSingleColorWithAlpha(getBaseShipImage(), color);
		BufferedImage imageUsedLife = RenderUtils.convertImageToSingleColorWithAlpha(getBaseShipImage(), Color.WHITE);
		int width = getBaseShipImage().getWidth(null);
		int radius = width / 2;
		double lifeScale = 0.5;
		double lifeWidth = width * lifeScale - 2;
		double lifeRadius = radius * lifeScale;
		AffineTransform trans = new AffineTransform();
		for (int i = 0; i < 5; i++) {
			if (i < lives) {
				trans.setToTranslation(x + (i * lifeWidth), y - lifeRadius);
				trans.scale(lifeScale, lifeScale);
				g2d.drawImage(imageLife, trans, null);
			} else if (i == lives && ship.isSpawning()) {
				double spawnScale = lifeScale * (1 - ship.spawnProgress());
				double scaledRadius = radius * spawnScale;
				trans.setToTranslation(x + (i * lifeWidth) + lifeRadius - scaledRadius, y - scaledRadius);
				trans.scale(spawnScale, spawnScale);
				g2d.drawImage(imageUsedLife, trans, null);
			} else {
				double scaledRadius = 1;
				trans.setToTranslation(x + (i * lifeWidth) + lifeRadius - scaledRadius, y - scaledRadius);
				trans.scale(scaledRadius / radius, scaledRadius / radius);
				g2d.drawImage(imageUsedLife, trans, null);
			}
		}
	}

	private static BufferedImage getBaseShipImage() {
		if (baseShipImage == null) {
			try {
				baseShipImage = ImageIO.read(Thread.currentThread().getContextClassLoader().getResource("img/ship.png"));
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		return baseShipImage;
	}

	static BufferedImage getPlayerShipImage(int playerId) {
		if (playerId == 0) {
			return getBaseShipImage();
		} else {
			return RenderUtils.applyHue(getBaseShipImage(), RenderUtils.PLAYER_HUES[playerId]);
		}
	}

	/**
	 * Applies the specified hue to an image.
	 * @param src The source image.
	 * @param hue The hue value between 0 and 360.
	 * @return A copy of the source image with the hue applied.
	 */
	private static BufferedImage applyHue(BufferedImage src, int hue) {
		BufferedImage filteredImage = new BufferedImage(src.getWidth(null), src.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < src.getWidth(); x++) {
			for (int y = 0; y < src.getHeight(); y++) {
				filteredImage.setRGB(x, y, adjustHue(new Color(src.getRGB(x, y), true), hue).getRGB());
			}
		}
		return filteredImage;
	}

	// All below code for changing hue was copied with minor modifications from
	// http://www.camick.com/java/source/HSLColor.java (blog post: https://tips4java.wordpress.com/2009/07/05/hsl-color)

	private static Color adjustHue(Color color, float degrees) {
		float[] hsl = fromRGB(color);
		return toRGB(degrees, hsl[1], hsl[2], color.getAlpha() / 255f);
	}

	private static float[] fromRGB(Color color) {
		// Get RGB values in the range 0 - 1
		float[] rgb = color.getRGBColorComponents(null);
		float r = rgb[0];
		float g = rgb[1];
		float b = rgb[2];
		// Minimum and Maximum RGB values are used in the HSL calculations
		float min = Math.min(r, Math.min(g, b));
		float max = Math.max(r, Math.max(g, b));
		// Calculate the Hue
		float h = 0;
		if (max == min) {
			h = 0;
		} else if (max == r) {
			h = ((60 * (g - b) / (max - min)) + 360) % 360;
		} else if (max == g) {
			h = (60 * (b - r) / (max - min)) + 120;
		} else if (max == b) {
			h = (60 * (r - g) / (max - min)) + 240;
		}
		// Calculate the Luminance
		float l = (max + min) / 2;
		// Calculate the Saturation
		float s = 0;
		if (max == min) {
			s = 0;
		} else if (l <= .5f) {
			s = (max - min) / (max + min);
		} else {
			s = (max - min) / (2 - max - min);
		}
		return new float[] { h, s * 100, l * 100 };
	}

	private static Color toRGB(float h, float s, float l, float alpha) {
		if (s < 0.0f || s > 100.0f) {
			String message = "Color parameter outside of expected range - Saturation";
			throw new IllegalArgumentException(message);
		}
		if (l < 0.0f || l > 100.0f) {
			String message = "Color parameter outside of expected range - Luminance";
			throw new IllegalArgumentException(message);
		}
		if (alpha < 0.0f || alpha > 1.0f) {
			String message = "Color parameter outside of expected range - Alpha";
			throw new IllegalArgumentException(message);
		}
		// Formula needs all values between 0 - 1.
		h = h % 360.0f;
		h /= 360f;
		s /= 100f;
		l /= 100f;
		float q = 0;
		if (l < 0.5) {
			q = l * (1 + s);
		} else {
			q = (l + s) - (s * l);
		}
		float p = 2 * l - q;
		float r = Math.max(0, HueToRGB(p, q, h + (1.0f / 3.0f)));
		float g = Math.max(0, HueToRGB(p, q, h));
		float b = Math.max(0, HueToRGB(p, q, h - (1.0f / 3.0f)));
		r = Math.min(r, 1.0f);
		g = Math.min(g, 1.0f);
		b = Math.min(b, 1.0f);
		return new Color(r, g, b, alpha);
	}

	private static float HueToRGB(float p, float q, float h) {
		if (h < 0) {
			h += 1;
		}
		if (h > 1) {
			h -= 1;
		}
		if (6 * h < 1) {
			return p + ((q - p) * 6 * h);
		}
		if (2 * h < 1) {
			return q;
		}
		if (3 * h < 2) {
			return p + ((q - p) * 6 * ((2.0f / 3.0f) - h));
		}
		return p;
	}
}
