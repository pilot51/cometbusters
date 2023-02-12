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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.UnknownHostException
import java.awt.Color as JavaColor
import java.awt.Font as JavaFont
import java.awt.Graphics as JavaGraphics
import java.awt.Graphics2D as JavaGraphics2D
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
		actual open class Image {
			val javaImage: JavaBufferedImage
			actual val width get() = javaImage.getWidth(null)
			actual val height get() = javaImage.getHeight(null)

			actual constructor(filePath: String, onLoad: ((Image) -> Unit)?) {
				javaImage = JavaImageIO.read(javaClass.getResource(filePath))
				onLoad?.let {
					it(this)
				}
			}

			protected actual constructor(image: Image) {
				val cm = image.javaImage.colorModel
				javaImage = JavaBufferedImage(
					cm, image.javaImage.copyData(null), cm.isAlphaPremultiplied, null
				)
			}
		}

		actual class MutableImage : Image {
			actual constructor(filePath: String, onLoad: ((Image) -> Unit)?) : super(filePath, onLoad)
			private actual constructor(orig: MutableImage) : super(orig)

			actual fun copy(): MutableImage = MutableImage(this)

			actual fun setSingleColor(rgb: Int) = setPixelColors { rgb }

			actual fun setHue(hue: Float) = setPixelColors {
				RenderUtils.adjustHue(it, hue)
			}

			private fun setPixelColors(convertPixelColor: (pixRgb: Int) -> Int) {
				for (x in 0 until width) {
					for (y in 0 until height) {
						val pixRgb = javaImage.getRGB(x, y)
						if (RenderUtils.getAlpha(pixRgb) == 0) continue
						javaImage.setRGB(x, y, convertPixelColor(pixRgb))
					}
				}
			}
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
				println("Starting host on port $PORT")
				CoroutineScope(Dispatchers.IO).launch {
					try {
						@Suppress("BlockingMethodInNonBlockingContext")
						serverSocket = JavaServerSocket(PORT)
					} catch (e: Exception) {
						println("Could not listen on port $PORT")
						return@launch
					}
					mpManager.connectionListener.onHostWaiting()
					var connectionsFull = false
					while (serverSocket != null) {
						if (connectionsFull) {
							if (connections.size < 3) {
								connectionsFull = false
								println("Connection dropped - resume listening for new connections")
							} else continue
						}
						try {
							@Suppress("BlockingMethodInNonBlockingContext")
							val conn = Connection(serverSocket!!.accept())
							connections.add(conn)
							mpManager.onConnected(conn.player)
							startListening(conn.player)
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
				println("Connecting to server: $address:$PORT")
				CoroutineScope(Dispatchers.IO).launch {
					try {
						val conn = Connection(address)
						connections.add(conn)
						conn.player.id = receive(conn.player)!!.toInt()
						mpManager.onConnected(conn.player)
						startListening(conn.player)
					} catch (e: UnknownHostException) {
						println("Unknown host: $address")
						mpManager.isClient = false
					} catch (e: IOException) {
						println("Connection failed!")
						e.printStackTrace()
						mpManager.isClient = false
					}
				}
			}

			private fun startListening(player: MultiplayerManager.RemotePlayer) {
				CoroutineScope(Dispatchers.IO).launch {
					while (mpManager.players.contains(player)) {
						val msg = receive(player)
						if (msg != null) {
							mpManager.onReceive(player, msg)
						} else {
							mpManager.disconnect(player)
							break
						}
					}
				}
			}

			/**
			 * Disconnects one connection.
			 * @param player The player to disconnect.
			 */
			actual fun disconnect(player: MultiplayerManager.RemotePlayer) {
				val conn = connections.find { it.player == player } ?: return
				if (!connections.remove(conn)) return
				CoroutineScope(Dispatchers.IO).launch {
					try {
						@Suppress("BlockingMethodInNonBlockingContext")
						conn.clientSocket.close()
					} catch (e: IOException) {
						e.printStackTrace()
					}
					if (connections.isEmpty() && serverSocket != null) {
						stopHost()
					}
					println("Player ${conn.player.id} disconnected from " +
						"${conn.ipRemote.hostAddress}:${conn.clientSocket.port}")
				}
			}

			/**
			 * Sends data to one connection.
			 * @param player The player to send the data to.
			 * @param data The data to be sent.
			 */
			actual fun send(player: MultiplayerManager.RemotePlayer, data: String) {
				val conn = connections.find { it.player == player } ?: return
				conn.out.println(data)
			}

			actual fun send(player: MultiplayerManager.RemotePlayer, playerId: Int) {
				val conn = connections.find { it.player == player } ?: return
				conn.out.println(playerId)
			}

			/**
			 * Receives data from a player.
			 * @param player The player to receive the data from.
			 */
			private fun receive(player: MultiplayerManager.RemotePlayer): String? {
				val conn = connections.find { it.player == player } ?: return null
				var msg: String? = null
				try {
					msg = conn.`in`.readLine()
				} catch (e: Exception) {
					e.printStackTrace()
				}
				return msg
			}

			private class Connection {
				val player = MultiplayerManager.RemotePlayer()
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
					println("Incoming connection: ${ipRemote.hostAddress}:${clientSocket.port}")
				}

				/** As client: Host connection */
				constructor(hostAddress: String) {
					ipRemote = JavaInetAddress.getByName(hostAddress)
					clientSocket = JavaSocket(hostAddress, PORT)
					out = JavaPrintWriter(clientSocket.getOutputStream(), true)
					`in` = JavaBufferedReader(JavaInputStreamReader(clientSocket.inputStream))
					println("Local client port: ${clientSocket.localPort}")
				}
			}

			actual companion object {
				actual val instance by lazy { ConnectionManager() }
				private val mpManager = MultiplayerManager.instance
				private const val PORT = 50001
			}
		}
	}
}
