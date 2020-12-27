/*
 * Copyright 2020 Mark Injerd
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

expect object Platform {
	object Utils {
		fun Int.toZeroPaddedString(length: Int): String

		object Math {
			fun toRadians(degrees: Double): Double
		}

		class Timer() {
			fun run(delay: Long, period: Long, execute: (Timer) -> Unit)
			fun cancel()
		}
	}

	object Resources {
		class Image {
			val width: Int
			val height: Int

			constructor(filePath: String, onLoad: ((Image) -> Unit)? = null)
			constructor(mutableImage: MutableImage)
		}

		class MutableImage {
			val width: Int
			val height: Int

			constructor(filePath: String)
			constructor(orig: MutableImage)

			fun toImmutableImage(): Image
			fun setSingleColor(rgb: Int)
			fun setHue(hue: Float)
		}

		class Font(name: String, style: Style, size: Int) {
			enum class Style {
				PLAIN, BOLD
			}

			companion object {
				val ARIAL: String
				val SANS_SERIF: String
			}
		}

		class Color(r: Int, g: Int, b: Int) {
			val rgba: Int

			companion object {
				val WHITE: Color
				val CYAN: Color
				val MAGENTA: Color
				val GREEN: Color
				val YELLOW: Color
			}
		}
	}

	object Renderer {
		class Transform2D() {
			fun setToTranslation(x: Double, y: Double)
			fun rotate(radians: Double, anchorX: Double, anchorY: Double)
			fun scale(sx: Double, sy: Double)
		}

		class RenderView2D {
			fun setColor(color: Resources.Color)
			fun setFont(font: Resources.Font)
			fun drawText(text: String, x: Int, y: Int)
			fun drawImage(image: Resources.Image, x: Int, y: Int)
			fun drawImage(image: Resources.Image, transform: Transform2D)
			fun drawImage(image: Resources.MutableImage, transform: Transform2D)
		}

		class Rectangle2D
	}

	object Network {
		class ConnectionManager {
			fun startHost()
			fun stopHost()
			fun connect(address: String)
			fun disconnect(mpConn: MultiplayerManager.PlayerConnection)
			fun send(mpConn: MultiplayerManager.PlayerConnection, data: String)
			fun send(mpConn: MultiplayerManager.PlayerConnection, playerId: Int)
			fun receive(mpConn: MultiplayerManager.PlayerConnection): String?
			companion object {
				val instance: ConnectionManager
			}
		}
	}
}
