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

import java.io.IOException
import javax.sound.sampled.*
import kotlin.concurrent.thread

actual class AudioPlayer actual constructor(
	private val audio: Audio,
	private val doLoop: Boolean
) {
	private var stop = false

	actual fun start() {
		thread(start = true) {
			val stream = try {
				AudioSystem.getAudioInputStream(javaClass.getResource(audio.filename))
			} catch (e: UnsupportedAudioFileException) {
				e.printStackTrace()
				return@thread
			} catch (e: IOException) {
				e.printStackTrace()
				return@thread
			}
			val format = stream.format
			val line: SourceDataLine
			try {
				line = AudioSystem.getLine(DataLine.Info(SourceDataLine::class.java, format)) as SourceDataLine
				line.open(format)
			} catch (e: LineUnavailableException) {
				e.printStackTrace()
				return@thread
			} catch (e: IllegalStateException) {
				e.printStackTrace()
				return@thread
			}
			line.start()
			var nBytesRead = 0
			val buffer = ByteArray(256)
			stream.mark(32000)
			try {
				while (nBytesRead != -1 && !stop) {
					nBytesRead = stream.read(buffer, 0, buffer.size)
					if (nBytesRead >= 0 && !stop) {
						line.write(buffer, 0, nBytesRead)
					} else if (doLoop && !stop) {
						if (stream.markSupported()) {
							stream.reset()
							nBytesRead = 0
						} else {
							start()
						}
					}
				}
			} catch (e: IOException) {
				e.printStackTrace()
				return@thread
			} finally {
				if (doLoop) {
					line.flush()
				} else {
					line.drain()
				}
				line.close()
			}
		}
	}

	actual fun stop() {
		stop = true
	}
}
