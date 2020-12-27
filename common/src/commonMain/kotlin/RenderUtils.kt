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

import Platform.Renderer.RenderView2D
import Platform.Renderer.Transform2D
import Platform.Resources.Color
import Platform.Resources.MutableImage
import kotlin.math.max
import kotlin.math.min

object RenderUtils {
	val PLAYER_COLORS = arrayOf(Color.CYAN, Color.MAGENTA, Color.GREEN, Color.YELLOW)
	private val PLAYER_HUES = intArrayOf(180, 300, 120, 60)
	val TEXT_LEVEL_COLOR = Color(255, 32, 128)
	val TEXT_GAMEOVER_COLOR = Color(32, 255, 64)
	private val baseShipImage = MutableImage("img/ship.png")

	/**
	 * Converts all pixels from the source image that aren't fully transparent to the specified color.
	 * @param src Source image to convert.
	 * @param fillColor The desired color.
	 * @return The converted image as a new [MutableImage].
	 */
	fun convertImageToSingleColorWithAlpha(src: MutableImage, fillColor: Color) =
		MutableImage(src).apply { setSingleColor(fillColor.rgba) }

	fun drawLives(view2D: RenderView2D, lives: Int, color: Color, x: Int, y: Int) {
		val ship = ShipManager.localShip
		val imageLife = convertImageToSingleColorWithAlpha(baseShipImage, color)
		val imageUsedLife = convertImageToSingleColorWithAlpha(baseShipImage, Color.WHITE)
		val width = baseShipImage.width
		val radius = width / 2
		val lifeScale = 0.5
		val lifeWidth = width * lifeScale - 2
		val lifeRadius = radius * lifeScale
		val trans = Transform2D()
		for (i in 0 until ship.maxLives) {
			if (i < lives) {
				trans.setToTranslation(x + i * lifeWidth, y - lifeRadius)
				trans.scale(lifeScale, lifeScale)
				view2D.drawImage(imageLife, trans)
			} else if (i == lives && ship.isSpawning) {
				val spawnScale = lifeScale * (1 - ship.spawnProgress())
				val scaledRadius = radius * spawnScale
				trans.setToTranslation(x + i * lifeWidth + lifeRadius - scaledRadius, y - scaledRadius)
				trans.scale(spawnScale, spawnScale)
				view2D.drawImage(imageUsedLife, trans)
			} else {
				val scaledRadius = 1.0
				trans.setToTranslation(x + i * lifeWidth + lifeRadius - scaledRadius, y - scaledRadius)
				trans.scale(scaledRadius / radius, scaledRadius / radius)
				view2D.drawImage(imageUsedLife, trans)
			}
		}
	}

	fun getPlayerShipImage(playerId: Int): MutableImage {
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
	private fun applyHue(src: MutableImage, hue: Int) = MutableImage(src).apply { setHue(hue.toFloat()) }

	// All below code for changing hue was copied with minor modifications from
	// http://www.camick.com/java/source/HSLColor.java (blog post: https://tips4java.wordpress.com/2009/07/05/hsl-color)
	fun adjustHue(rgba: Int, degrees: Float): Int {
		val hsl = rgbToHsl(rgba)
		return hslToRgb(degrees, hsl[1], hsl[2], getAlpha(rgba) / 255f)
	}

	fun getAlpha(rgba: Int) = rgba shr 24 and 0xff

	fun rgbaToIntArray(rgba: Int) = intArrayOf(
		rgba shr 16 and 0xff, // r
		rgba shr 8 and 0xff, // g
		rgba shr 0 and 0xff, // b
		rgba shr 24 and 0xff // a
	)

	fun rgbaToFloatArray(rgba: Int) = floatArrayOf(
		(rgba shr 16 and 0xff) / 255f, // r
		(rgba shr 8 and 0xff) / 255f, // g
		(rgba shr 0 and 0xff) / 255f, // b
		(rgba shr 24 and 0xff) / 255f // a
	)

	fun rgbaComponentsToInt(r: Int, g: Int, b: Int, a: Int): Int {
		return (a and 0xFF shl 24) or
			(r and 0xFF shl 16) or
			(g and 0xFF shl 8) or
			(b and 0xFF shl 0)
	}

	fun rgbaFloatComponentsToInt(r: Float, g: Float, b: Float, a: Float): Int {
		return ((a * 255).toInt() and 0xFF shl 24) or
			((r * 255).toInt() and 0xFF shl 16) or
			((g * 255).toInt() and 0xFF shl 8) or
			((b * 255).toInt() and 0xFF shl 0)
	}

	private fun componentToHex(c: Int): String {
		val hex = c.toString(16)
		return if (hex.length == 1) "0$hex" else hex
	}

	fun rgbaToHex(r: Int, g: Int, b: Int, a: Int = 255) =
		"#${componentToHex(r)}${componentToHex(g)}${componentToHex(b)}${componentToHex(a)}"

	private fun rgbToHsl(rgb: Int): FloatArray {
		// Get RGB values in the range 0 - 1
		val rgbFloats = rgbaToFloatArray(rgb)
		val r = rgbFloats[0]
		val g = rgbFloats[1]
		val b = rgbFloats[2]
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

	private fun hslToRgb(hue: Float, sat: Float, lum: Float, alpha: Float): Int {
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
		return rgbaFloatComponentsToInt(r, g, b, alpha)
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
