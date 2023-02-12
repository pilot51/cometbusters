/*
 * Copyright 2020-2023 Mark Injerd
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

import kotlinx.browser.document
import kotlinx.browser.window
import network.Peer
import org.khronos.webgl.Uint16Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import kotlin.math.PI
import org.w3c.dom.CanvasRenderingContext2D as JsCanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement as JsHTMLCanvasElement
import org.w3c.dom.Image as JsImage
import org.w3c.dom.TextMetrics as JsTextMetrics

actual object Platform {
	actual object Utils {
		actual fun Int.toZeroPaddedString(length: Int) = toString().padStart(length, "0".single())

		actual object Math {
			actual fun toRadians(degrees: Double) = degrees / 180.0 * PI
		}

		actual class Timer {
			private var id: Int = 0

			actual fun run(delay: Long, period: Long, execute: (Timer) -> Unit) {
				id = window.setInterval({ execute(this) }, period.toInt())
			}

			actual fun cancel() {
				window.clearInterval(id)
			}
		}
	}

	actual object Resources {
		actual open class Image {
			val jsImage: JsImage
			var jsCanvas: JsHTMLCanvasElement? = null
			actual val width get() = jsImage.width
			actual val height get() = jsImage.height

			actual constructor(filePath: String, onLoad: ((Image) -> Unit)?) {
				jsImage = JsImage().apply {
					src = filePath
					onLoad?.let {
						onload = { it(this@Image) }
					}
				}
			}

			protected actual constructor(image: Image) {
				jsImage = image.jsImage.cloneNode(true) as JsImage
				jsCanvas = image.jsCanvas
			}
		}

		actual class MutableImage : Image {
			actual constructor(filePath: String, onLoad: ((Image) -> Unit)?) : super(filePath, onLoad)
			private actual constructor(orig: MutableImage) : super(orig)

			actual fun copy(): MutableImage = MutableImage(this)

			actual fun setSingleColor(rgb: Int) {
				val rgbArray = RenderUtils.rgbaToIntArray(rgb)
				setPixelColors { _, _ -> rgbArray }
			}

			actual fun setHue(hue: Float) = setPixelColors { data, i ->
				val rgba = RenderUtils.rgbaComponentsToInt(
					data[i + 0].toInt(),
					data[i + 1].toInt(),
					data[i + 2].toInt(),
					data[i + 3].toInt()
				)
				RenderUtils.rgbaToIntArray(RenderUtils.adjustHue(rgba, hue))
			}

			private fun setPixelColors(
				convertPixelColor: (data: Uint16Array, index: Int) -> IntArray
			) {
				val view = Renderer.RenderView2D(width, height)
				jsCanvas = view.jsCanvas
				view.jsCanvas2D.drawImage(jsImage, 0.0, 0.0)
				val imageData = view.jsCanvas2D.getImageData(0.0, 0.0, width.toDouble(), height.toDouble())
				val data = imageData.data.unsafeCast<Uint16Array>()
				for (i in 0 until data.length step 4) {
					if (data[i + 3] == 0.toShort()) continue
					val colorArray = convertPixelColor(data, i)
					data[i] = colorArray[0].toShort() // red
					data[i + 1] = colorArray[1].toShort() // green
					data[i + 2] = colorArray[2].toShort() // blue
				}
				view.jsCanvas2D.putImageData(imageData, 0.0, 0.0)
			}
		}

		actual class Font actual constructor(
			private val name: String,
			private val style: Style,
			private val size: Int
		) {
			override fun toString() = "${style.jsWeight} ${size}px $name"

			actual enum class Style(val jsWeight: String) {
				PLAIN("normal"),
				BOLD("bold");
			}

			actual companion object {
				actual val ARIAL = "Arial"
				actual val SANS_SERIF = "SansSerif"
			}
		}

		actual class Color constructor(
			val jsColor: String
		) {
			actual val rgba: Int by lazy {
				val ctx = (document.createElement("canvas") as JsHTMLCanvasElement)
					.getContext("2d") as JsCanvasRenderingContext2D
				ctx.fillStyle = jsColor
				val hexColor = ctx.fillStyle as String
				hexColor.removePrefix("#").toInt(16)
			}

			actual constructor(r: Int, g: Int, b: Int) : this(RenderUtils.rgbaToHex(r, g, b))

			actual companion object {
				actual val WHITE: Color = Color("white")
				actual val CYAN: Color = Color("cyan")
				actual val MAGENTA: Color = Color("magenta")
				actual val GREEN: Color = Color("green")
				actual val YELLOW: Color = Color("yellow")
			}
		}
	}

	actual object Renderer {
		actual class Transform2D {
			val affineTransform = AffineTransform()

			actual fun setToTranslation(x: Double, y: Double) {
				affineTransform.setToTranslation(x, y)
			}

			actual fun rotate(radians: Double, anchorX: Double, anchorY: Double) {
				affineTransform.rotate(radians, anchorX, anchorY)
			}

			actual fun scale(sx: Double, sy: Double) {
				affineTransform.scale(sx, sy)
			}
		}

		actual class RenderView2D(width: Int, height: Int) {
			val jsCanvas = (document.createElement("canvas") as JsHTMLCanvasElement).also {
				it.width = width
				it.height = height
			}
			val jsCanvas2D = jsCanvas.getContext("2d") as JsCanvasRenderingContext2D

			actual fun setFont(font: Resources.Font) {
				jsCanvas2D.font = font.toString()
			}

			actual fun setColor(color: Resources.Color) {
				jsCanvas2D.fillStyle = color.jsColor
			}

			actual fun drawText(text: String, x: Int, y: Int) {
				jsCanvas2D.fillText(text, x.toDouble(), y.toDouble())
			}

			actual fun drawImage(image: Resources.Image, x: Int, y: Int) {
				jsCanvas2D.drawImage(image.jsImage, x.toDouble(), y.toDouble())
			}

			actual fun drawImage(image: Resources.Image, transform: Transform2D) {
				val xform = transform.affineTransform
				jsCanvas2D.save()
				jsCanvas2D.transform(xform.scaleX, xform.shearY, xform.shearX, xform.scaleY,
					xform.translateX, xform.translateY)
				jsCanvas2D.drawImage(image.jsCanvas ?: image.jsImage, 0.0, 0.0)
				jsCanvas2D.restore()
			}
		}

		actual class Rectangle2D(val width: Double, val height: Double) {
			constructor(textMetrics: JsTextMetrics) : this(textMetrics.width,
				textMetrics.fontBoundingBoxAscent + textMetrics.fontBoundingBoxDescent)

			companion object {
				fun getTextRect(text: String, font: Resources.Font): Rectangle2D {
					val jsCanvas = document.createElement("canvas") as JsHTMLCanvasElement
					val jsCanvas2D = jsCanvas.getContext("2d") as JsCanvasRenderingContext2D
					jsCanvas2D.font = font.toString()
					return Rectangle2D(jsCanvas2D.measureText(text))
				}
			}
		}
	}

	actual object Network {
		actual class ConnectionManager {
			private val connections: MutableList<Connection> = ArrayList()
			private lateinit var peer: Peer
			private lateinit var onHostId: ((hostId: String?) -> Unit)

			actual fun startHost() {
				println("Starting host")
				peer = Peer().apply {
					hook.onOpen { sessionId ->
						onHostId(sessionId)
					}
					hook.onConnected { sessionId ->
						println("Incoming connection: $sessionId")
						val conn = Connection(sessionId)
						connections.add(conn)
						mpManager.onConnected(conn.player)
						startListening(conn.player)
					}
					hook.onErrorConnecting {
						println("Incoming client connection failed")
					}
					hook.onDisconnected { sessionId ->
						mpManager.disconnect(connections.single { it.sessionId == sessionId }.player)
					}
				}
				mpManager.connectionListener.onHostWaiting()
			}

			fun onHostId(handler: (hostId: String?) -> Unit) {
				onHostId = handler
			}

			actual fun stopHost() {
				onHostId(null)
				peer.disconnect()
			}

			actual fun connect(address: String) {
				println("Connecting to server: $address")
				peer = Peer().apply {
					hook.onOpen {
						connect(address)
					}
					hook.onConnected { sessionId ->
						onHostId(sessionId)
						val conn = Connection(sessionId)
						connections.add(conn)
						if (mpManager.isClient) {
							hook.onReceiveData { playerId ->
								hook.onReceiveData(null)
								conn.player.id = playerId.toInt()
								mpManager.onConnected(conn.player)
								startListening(conn.player)
							}
						}
					}
					hook.onErrorConnecting {
						println("Failed to connect to server")
						mpManager.isClient = false
					}
					hook.onDisconnected { sessionId ->
						mpManager.disconnect(connections.single { it.sessionId == sessionId }.player)
					}
				}
			}

			private fun startListening(player: MultiplayerManager.RemotePlayer) {
				peer.hook.onReceiveData {
					mpManager.onReceive(player, it)
				}
			}

			actual fun disconnect(player: MultiplayerManager.RemotePlayer) {
				val conn = connections.find { it.player == player } ?: return
				if (!connections.remove(conn)) return
				peer.disconnect(conn.sessionId)
				if (connections.isEmpty() && !mpManager.isClient) {
					stopHost()
				}
				println("Player ${player.id} disconnected from ${conn.sessionId}")
			}

			actual fun send(player: MultiplayerManager.RemotePlayer, data: String) {
				val conn = connections.find { it.player == player } ?: return
				peer.send(conn.sessionId, data)
			}

			actual fun send(player: MultiplayerManager.RemotePlayer, playerId: Int) {
				val conn = connections.find { it.player == player } ?: return
				peer.send(conn.sessionId, playerId.toString())
			}

			private class Connection(val sessionId: String) {
				val player = MultiplayerManager.RemotePlayer()
			}

			actual companion object {
				actual val instance by lazy { ConnectionManager() }
				private val mpManager = MultiplayerManager.instance
			}
		}
	}
}
