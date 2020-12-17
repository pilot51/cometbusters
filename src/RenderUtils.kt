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

import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min

object RenderUtils {
	val PLAYER_COLORS = arrayOf(Color.CYAN, Color.MAGENTA, Color.GREEN, Color.YELLOW)
	private val PLAYER_HUES = intArrayOf(180, 300, 120, 60)
	val TEXT_LEVEL_COLOR = Color(255, 32, 128)
	val TEXT_GAMEOVER_COLOR = Color(32, 255, 64)
	private var baseShipImage: BufferedImage? = null
		get() {
			if (field == null) {
				field = try {
					ImageIO.read(RenderUtils::class.java.getResource("ship.png"))
				} catch (e: IOException) {
					e.printStackTrace()
					return null
				}
			}
			return field
		}

	/**
	 * Converts all pixels from the source image that aren't fully transparent to the specified color.
	 * @param src Source image to convert.
	 * @param fillColor The desired color.
	 * @return The converted image as a new [BufferedImage].
	 */
	fun convertImageToSingleColorWithAlpha(src: BufferedImage?, fillColor: Color?): BufferedImage {
		val newImage = BufferedImage(src!!.getWidth(null), src.getHeight(null), BufferedImage.TYPE_INT_ARGB)
		for (x in 0 until src.width) {
			for (y in 0 until src.height) {
				if (Color(src.getRGB(x, y), true).alpha == 255) {
					newImage.setRGB(x, y, fillColor!!.rgb)
				}
			}
		}
		return newImage
	}

	fun drawLives(g2d: Graphics2D, lives: Int, color: Color?, x: Int, y: Int) {
		val ship = ShipManager.localShip
		val imageLife = convertImageToSingleColorWithAlpha(baseShipImage, color)
		val imageUsedLife = convertImageToSingleColorWithAlpha(baseShipImage, Color.WHITE)
		val width = baseShipImage!!.getWidth(null)
		val radius = width / 2
		val lifeScale = 0.5
		val lifeWidth = width * lifeScale - 2
		val lifeRadius = radius * lifeScale
		val trans = AffineTransform()
		for (i in 0 until ship.maxLives) {
			if (i < lives) {
				trans.setToTranslation(x + i * lifeWidth, y - lifeRadius)
				trans.scale(lifeScale, lifeScale)
				g2d.drawImage(imageLife, trans, null)
			} else if (i == lives && ship.isSpawning) {
				val spawnScale = lifeScale * (1 - ship.spawnProgress())
				val scaledRadius = radius * spawnScale
				trans.setToTranslation(x + i * lifeWidth + lifeRadius - scaledRadius, y - scaledRadius)
				trans.scale(spawnScale, spawnScale)
				g2d.drawImage(imageUsedLife, trans, null)
			} else {
				val scaledRadius = 1.0
				trans.setToTranslation(x + i * lifeWidth + lifeRadius - scaledRadius, y - scaledRadius)
				trans.scale(scaledRadius / radius, scaledRadius / radius)
				g2d.drawImage(imageUsedLife, trans, null)
			}
		}
	}

	fun getPlayerShipImage(playerId: Int): BufferedImage? {
		return if (playerId == 0) {
			baseShipImage
		} else {
			applyHue(baseShipImage, PLAYER_HUES[playerId])
		}
	}

	/**
	 * Applies the specified hue to an image.
	 * @param src The source image.
	 * @param hue The hue value between 0 and 360.
	 * @return A copy of the source image with the hue applied.
	 */
	private fun applyHue(src: BufferedImage?, hue: Int): BufferedImage {
		val filteredImage = BufferedImage(src!!.getWidth(null), src.getHeight(null), BufferedImage.TYPE_INT_ARGB)
		for (x in 0 until src.width) {
			for (y in 0 until src.height) {
				filteredImage.setRGB(x, y, adjustHue(Color(src.getRGB(x, y), true), hue.toFloat()).rgb)
			}
		}
		return filteredImage
	}

	// All below code for changing hue was copied with minor modifications from
	// http://www.camick.com/java/source/HSLColor.java (blog post: https://tips4java.wordpress.com/2009/07/05/hsl-color)
	private fun adjustHue(color: Color, degrees: Float): Color {
		val hsl = fromRGB(color)
		return toRGB(degrees, hsl[1], hsl[2], color.alpha / 255f)
	}

	private fun fromRGB(color: Color): FloatArray {
		// Get RGB values in the range 0 - 1
		val rgb = color.getRGBColorComponents(null)
		val r = rgb[0]
		val g = rgb[1]
		val b = rgb[2]
		// Minimum and Maximum RGB values are used in the HSL calculations
		val min = min(r, min(g, b))
		val max = max(r, max(g, b))
		// Calculate the Hue
		val h = when (max) {
			min -> 0f
			r -> (60 * (g - b) / (max - min) + 360) % 360
			g -> 60 * (b - r) / (max - min) + 120
			b -> 60 * (r - g) / (max - min) + 240
			else -> 0f
		}
		// Calculate the Luminance
		val l = (max + min) / 2
		// Calculate the Saturation
		val s = when {
			max == min -> 0f
			l <= .5f -> (max - min) / (max + min)
			else -> (max - min) / (2 - max - min)
		}
		return floatArrayOf(h, s * 100, l * 100)
	}

	private fun toRGB(hue: Float, sat: Float, lum: Float, alpha: Float): Color {
		var h = hue
		var s = sat
		var l = lum
		if (s < 0.0f || s > 100.0f) {
			val message = "Color parameter outside of expected range - Saturation"
			throw IllegalArgumentException(message)
		}
		if (l < 0.0f || l > 100.0f) {
			val message = "Color parameter outside of expected range - Luminance"
			throw IllegalArgumentException(message)
		}
		if (alpha < 0.0f || alpha > 1.0f) {
			val message = "Color parameter outside of expected range - Alpha"
			throw IllegalArgumentException(message)
		}
		// Formula needs all values between 0 - 1.
		h %= 360.0f
		h /= 360f
		s /= 100f
		l /= 100f
		val q = if (l < 0.5) {
			l * (1 + s)
		} else {
			l + s - s * l
		}
		val p = 2 * l - q
		var r = max(0f, hueToRGB(p, q, h + 1.0f / 3.0f))
		var g = max(0f, hueToRGB(p, q, h))
		var b = max(0f, hueToRGB(p, q, h - 1.0f / 3.0f))
		r = min(r, 1.0f)
		g = min(g, 1.0f)
		b = min(b, 1.0f)
		return Color(r, g, b, alpha)
	}

	private fun hueToRGB(p: Float, q: Float, h: Float): Float {
		var h2 = h
		if (h2 < 0) {
			h2 += 1f
		}
		if (h2 > 1) {
			h2 -= 1f
		}
		if (6 * h2 < 1) {
			return p + (q - p) * 6 * h2
		}
		if (2 * h2 < 1) {
			return q
		}
		return if (3 * h2 < 2) {
			p + (q - p) * 6 * (2.0f / 3.0f - h2)
		} else p
	}
}
