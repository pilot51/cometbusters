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

import java.io.IOException
import javax.sound.sampled.*

enum class Audio(private val filename: String) {
	MUSIC_GAME("comet.mid"),
	MUSIC_DEATH("comet1.mid"),
	MUSIC_HIGHSCORE("comet2.mid"),
	THRUST("thrust.wav"),
	SHOOT("shoot.wav"),
	SPAWN("spawn.wav"),
	EXPLODE_PLAYER("explode_player.wav"),
	EXPLODE_LARGE("explode_large.wav"),
	EXPLODE_MEDIUM("explode_medium.wav"),
	EXPLODE_SMALL("explode_small.wav"),
	EXTRA_LIFE("extra_life.wav");

	private var player: AudioPlayer? = null
	private val isMusic = filename.endsWith(".mid")

	/**
	 * Plays this media if the respective sound or music setting is enabled. If the media is music, any other music playing is stopped first.
	 * @param loop True to loop the sound until stopped, false to play it only once.
	 */
	@JvmOverloads
	fun play(loop: Boolean = false) {
		if (if (isMusic) isMusicEnabled else isSoundEnabled) {
			if (isMusic) {
				stopAll(true)
			}
			if (loop && player != null) {
				player!!.stop = true
			}
			player = AudioPlayer(loop)
			player!!.start()
		}
	}

	/** Convenience for [play], passing true for loop. */
	fun loop() {
		play(true)
	}

	fun stop() {
		if (equals(THRUST)) {
			for (ship in ShipManager.ships) {
				if (ship == null) continue
				if (ship.isAccelerating) return
			}
		}
		player?.run {
			stop = true
			player = null
		}
	}

	private inner class AudioPlayer(private val doLoop: Boolean) : Thread() {
		var stop = false

		override fun run() {
			val stream = try {
				AudioSystem.getAudioInputStream(javaClass.getResource(filename))
			} catch (e: UnsupportedAudioFileException) {
				e.printStackTrace()
				return
			} catch (e: IOException) {
				e.printStackTrace()
				return
			}
			val format = stream.format
			val line: SourceDataLine
			try {
				line = AudioSystem.getLine(DataLine.Info(SourceDataLine::class.java, format)) as SourceDataLine
				line.open(format)
			} catch (e: LineUnavailableException) {
				e.printStackTrace()
				return
			} catch (e: IllegalStateException) {
				e.printStackTrace()
				return
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
							loop()
						}
					}
				}
			} catch (e: IOException) {
				e.printStackTrace()
				return
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

	companion object {
		fun init() {
			values()
		}

		private var isSoundEnabled = true
		private var isMusicEnabled = true

		/** @return True if sound has been enabled, false if disabled. */
		fun toggleSound(): Boolean {
			isSoundEnabled = isSoundEnabled xor true
			if (!isSoundEnabled) {
				stopAll(false)
			}
			return isSoundEnabled
		}

		/** @return True if music has been enabled, false if disabled. */
		fun toggleMusic(): Boolean {
			isMusicEnabled = isMusicEnabled xor true
			if (!isMusicEnabled) {
				stopAll(true)
			}
			return isMusicEnabled
		}

		/**
		 * Stops all sound effects or music.
		 * @param music True to stop music, false to stop sound effects.
		 */
		private fun stopAll(music: Boolean) {
			for (s in values()) {
				if (s.isMusic == music) {
					s.stop()
				}
			}
		}
	}
}
