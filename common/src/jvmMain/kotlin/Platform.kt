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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Color as JavaColor
import java.awt.Font as JavaFont
import java.awt.Graphics as JavaGraphics
import java.awt.Graphics2D as JavaGraphics2D
import java.awt.Image as JavaImage
import java.awt.font.FontRenderContext as JavaFontRenderContext
import java.awt.font.TextLayout as JavaTextLayout
import java.awt.geom.AffineTransform as JavaAffineTransform
import java.awt.geom.Rectangle2D as JavaRectangle2D
import java.awt.image.BufferedImage as JavaBufferedImage
import java.io.BufferedReader as JavaBufferedReader
import java.io.InputStreamReader as JavaInputStreamReader
import java.io.PrintWriter as JavaPrintWriter
import java.lang.Math as JavaMath
import java.net.InetAddress as JavaInetAddress
import java.net.ServerSocket as JavaServerSocket
import java.net.Socket as JavaSocket
import java.util.Timer as JavaTimer
import java.util.TimerTask as JavaTimerTask
import javax.imageio.ImageIO as JavaImageIO

actual object Platform {
	actual object Utils {
		actual fun Int.toZeroPaddedString(length: Int) = String.format("%0${length}d", this)

		actual object Math {
			actual fun toRadians(degrees: Double) = JavaMath.toRadians(degrees)
		}

		actual class Timer {
			private lateinit var timerTask: JavaTimerTask

			actual fun run(delay: Long, period: Long, execute: (Timer) -> Unit) {
				timerTask = object : JavaTimerTask() {
					override fun run() {
						execute(this@Timer)
					}
				}
				JavaTimer().schedule(timerTask, delay, period)
			}

			actual fun cancel() {
				timerTask.cancel()
			}
		}
	}

	actual object Resources {
		actual class Image {
			val javaImage: JavaImage
			actual val width get() = javaImage.getWidth(null)
			actual val height get() = javaImage.getHeight(null)

			actual constructor(filePath: String, onLoad: ((Image) -> Unit)?) {
				javaImage = JavaImageIO.read(javaClass.getResource(filePath))
				onLoad?.let {
					it(this)
				}
			}

			actual constructor(mutableImage: MutableImage) {
				javaImage = mutableImage.javaImage
			}
		}

		actual class MutableImage {
			val javaImage: JavaBufferedImage

			actual constructor(filePath: String) {
				javaImage = JavaImageIO.read(javaClass.getResource(filePath))
			}

			actual constructor(orig: MutableImage) {
				val cm = orig.javaImage.colorModel
				javaImage = JavaBufferedImage(cm, orig.javaImage.copyData(null), cm.isAlphaPremultiplied, null)
			}

			actual val width get() = javaImage.width
			actual val height get() = javaImage.height

			actual fun setSingleColor(rgb: Int) {
				for (x in 0 until width) {
					for (y in 0 until height) {
						if (RenderUtils.getAlpha(javaImage.getRGB(x, y)) == 0) continue
						javaImage.setRGB(x, y, rgb)
					}
				}
			}
			actual fun setHue(hue: Float) {
				for (x in 0 until width) {
					for (y in 0 until height) {
						val rgb = javaImage.getRGB(x, y)
						if (RenderUtils.getAlpha(rgb) == 0) continue
						javaImage.setRGB(x, y, RenderUtils.adjustHue(rgb, hue))
					}
				}
			}

			actual fun toImmutableImage() = Image(this)
		}

		actual class Font actual constructor(name: String, style: Style, size: Int) {
			val javaFont = JavaFont(name, style.javaStyle, size)

			actual enum class Style(val javaStyle: Int) {
				PLAIN(JavaFont.PLAIN),
				BOLD(JavaFont.BOLD);
			}

			actual companion object {
				actual val ARIAL = "Arial"
				actual val SANS_SERIF = JavaFont.SANS_SERIF
			}
		}

		actual class Color constructor(val javaColor: JavaColor) {
			actual val rgba = javaColor.rgb

			actual constructor(r: Int, g: Int, b: Int) : this(JavaColor(r, g, b))

			actual companion object {
				actual val WHITE: Color = Color(JavaColor.WHITE)
				actual val CYAN: Color = Color(JavaColor.CYAN)
				actual val MAGENTA: Color = Color(JavaColor.MAGENTA)
				actual val GREEN: Color = Color(JavaColor.GREEN)
				actual val YELLOW: Color = Color(JavaColor.YELLOW)
			}
		}
	}

	actual object Renderer {
		actual class Transform2D {
			val javaTransform = JavaAffineTransform()

			actual fun setToTranslation(x: Double, y: Double) {
				javaTransform.setToTranslation(x, y)
			}

			actual fun rotate(radians: Double, anchorX: Double, anchorY: Double) {
				javaTransform.rotate(radians, anchorX, anchorY)
			}

			actual fun scale(sx: Double, sy: Double) {
				javaTransform.scale(sx, sy)
			}
		}

		actual class RenderView2D {
			private val javaGraphics2D: JavaGraphics2D
			val fontRenderContext get() = FontRenderContext(javaGraphics2D.fontRenderContext)

			constructor(javaGraphics: JavaGraphics) {
				javaGraphics2D = javaGraphics as JavaGraphics2D
			}

			actual fun setColor(color: Resources.Color) {
				javaGraphics2D.color = color.javaColor
			}

			actual fun setFont(font: Resources.Font) {
				javaGraphics2D.font = font.javaFont
			}

			actual fun drawText(text: String, x: Int, y: Int) {
				javaGraphics2D.drawString(text, x, y)
			}

			actual fun drawImage(image: Resources.Image, x: Int, y: Int) {
				javaGraphics2D.drawImage(image.javaImage, x, y, null)
			}

			actual fun drawImage(image: Resources.Image, transform: Transform2D) {
				javaGraphics2D.drawImage(image.javaImage, transform.javaTransform, null)
			}

			actual fun drawImage(image: Resources.MutableImage, transform: Transform2D) {
				javaGraphics2D.drawImage(image.javaImage, transform.javaTransform, null)
			}
		}

		class FontRenderContext(val javaFontRenderContext: JavaFontRenderContext)

		class TextLayout(javaTextLayout: JavaTextLayout) {
			val bounds = Rectangle2D(javaTextLayout.bounds)

			constructor(text: String, font: Resources.Font, fontRenderContext: FontRenderContext) :
				this(JavaTextLayout(text, font.javaFont, fontRenderContext.javaFontRenderContext))
		}

		actual class Rectangle2D(javaRectangle2D: JavaRectangle2D) {
			val width = javaRectangle2D.width
			val height = javaRectangle2D.height
		}
	}

	actual object Network {
		actual class ConnectionManager private constructor() {
			private val connections: MutableList<Connection> = ArrayList()
			private var serverSocket: JavaServerSocket? = null

			actual fun startHost() {
				CoroutineScope(Dispatchers.IO).launch {
					try {
						serverSocket = JavaServerSocket(MultiplayerManager.PORT)
					} catch (e: Exception) {
						println("Could not listen on port ${MultiplayerManager.PORT}")
						return@launch
					}
					MultiplayerManager.instance.connectionListener.onHostWaiting()
					var connectionsFull = false
					while (serverSocket != null) {
						if (connectionsFull) {
							if (connections.size < 3) {
								connectionsFull = false
								println("Connection dropped - resume listening for new connections")
							} else continue
						}
						try {
							val conn = Connection(serverSocket!!.accept())
							connections.add(conn)
							MultiplayerManager.instance.onConnected(conn.mpConn)
						} catch (e: Exception) {
							if (serverSocket != null) {
								println("Incoming client connection failed")
								e.printStackTrace()
							}
						}
						if (connections.size == 3) {
							connectionsFull = true
							println("Connections full - waiting for a connection to drop before listening for new connections")
						}
					}
				}
			}

			actual fun stopHost() {
				try {
					serverSocket!!.close()
					serverSocket = null
					println("Stopped host")
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}

			actual fun connect(address: String) {
				CoroutineScope(Dispatchers.IO).launch {
					try {
						val conn = Connection(address)
						connections.add(conn)
						MultiplayerManager.instance.onConnected(conn.mpConn)
					} catch (e: Exception) {
						println("Failed to connect to server")
						MultiplayerManager.instance.isClient = false
						e.printStackTrace()
					}
				}
			}

			actual fun disconnect(mpConn: MultiplayerManager.PlayerConnection) {
				val conn = connections.find { it.ipRemote.hostAddress == mpConn.remoteAddress } ?: return
				disconnect(conn)
			}

			/**
			 * Disconnects one connection.
			 * @param conn The connection to disconnect.
			 */
			private fun disconnect(conn: Connection) {
				if (!connections.remove(conn)) return
				CoroutineScope(Dispatchers.IO).launch {
					try {
						conn.clientSocket.close()
					} catch (e: Exception) {
						e.printStackTrace()
					}
					if (connections.isEmpty() && serverSocket != null) {
						stopHost()
					}
				}
			}

			/**
			 * Sends data to one connection.
			 * @param mpConn The connection to send the data to.
			 * @param data The data to be sent.
			 */
			actual fun send(mpConn: MultiplayerManager.PlayerConnection, data: String) {
				val conn = connections.find { it.ipRemote.hostAddress == mpConn.remoteAddress } ?: return
				send(conn, data)
			}

			private fun send(conn: Connection, data: String) {
				conn.out.println(data)
			}

			actual fun send(mpConn: MultiplayerManager.PlayerConnection, playerId: Int) {
				val conn = connections.find { it.ipRemote.hostAddress == mpConn.remoteAddress } ?: return
				send(conn, playerId)
			}

			private fun send(conn: Connection, playerId: Int) {
				conn.out.println(playerId)
			}

			actual fun receive(mpConn: MultiplayerManager.PlayerConnection): String? {
				val conn = connections.find { it.ipRemote.hostAddress == mpConn.remoteAddress } ?: return null
				return receive(conn)
			}

			/**
			 * Receives data from a connection.
			 * @param conn The connection to receive the data from.
			 */
			private fun receive(conn: Connection): String? {
				var msg: String? = null
				try {
					msg = conn.`in`.readLine()
				} catch (e: Exception) {
					e.printStackTrace()
				}
				return msg
			}

			private class Connection {
				val mpConn: MultiplayerManager.PlayerConnection
				val clientSocket: JavaSocket
				val out: JavaPrintWriter
				val `in`: JavaBufferedReader
				val ipRemote: JavaInetAddress

				/** As host: Incoming client connection */
				constructor(clientSocket: JavaSocket) {
					this.clientSocket = clientSocket
					out = JavaPrintWriter(clientSocket.getOutputStream(), true)
					`in` = JavaBufferedReader(JavaInputStreamReader(clientSocket.getInputStream()))
					ipRemote = clientSocket.inetAddress
					mpConn = MultiplayerManager.PlayerConnection(
						ipRemote.hostAddress, clientSocket.port, MultiplayerManager.PORT)
				}

				/** As client: Host connection */
				constructor(hostAddress: String) {
					ipRemote = JavaInetAddress.getByName(hostAddress)
					clientSocket = JavaSocket(hostAddress, MultiplayerManager.PORT)
					out = JavaPrintWriter(clientSocket.getOutputStream(), true)
					`in` = JavaBufferedReader(JavaInputStreamReader(clientSocket.inputStream))
					mpConn = MultiplayerManager.PlayerConnection(
						ipRemote.hostAddress, MultiplayerManager.PORT, clientSocket.localPort)
				}
			}

			actual companion object {
				actual val instance by lazy { ConnectionManager() }
			}
		}
	}
}
