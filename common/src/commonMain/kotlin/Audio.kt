/*
 * Copyright 2013-2023 Mark Injerd
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

enum class Audio(
	val filename: String
) {
	MUSIC_GAME("snd/comet.mid"),
	MUSIC_DEATH("snd/comet1.mid"),
	MUSIC_HIGHSCORE("snd/comet2.mid"),
	THRUST("snd/thrust.wav"),
	SHOOT("snd/shoot.wav"),
	SPAWN("snd/spawn.wav"),
	EXPLODE_PLAYER("snd/explode_player.wav"),
	EXPLODE_LARGE("snd/explode_large.wav"),
	EXPLODE_MEDIUM("snd/explode_medium.wav"),
	EXPLODE_SMALL("snd/explode_small.wav"),
	EXTRA_LIFE("snd/extra_life.wav");

	private var player: AudioPlayer? = null
	val isMusic = filename.endsWith(".mid")

	/**
	 * Plays this media if the respective sound or music setting is enabled. If the media is music, any other music playing is stopped first.
	 * @param loop True to loop the sound until stopped, false to play it only once.
	 */
	fun play(loop: Boolean = false) {
		if (if (isMusic) isMusicEnabled else isSoundEnabled) {
			if (isMusic) {
				stopAll(true)
			}
			if (loop) player?.stop()
			player = AudioPlayer(this, loop).apply { start() }
		}
	}

	/** Convenience for [play], passing true for loop. */
	fun loop() {
		play(true)
	}

	fun stop() {
		if (equals(THRUST)) {
			ShipManager.ships.filterNotNull().forEach {
				if (it.isAccelerating) return
			}
		}
		player?.run {
			stop()
			player = null
		}
	}

	companion object {
		var isSoundEnabled = true
			private set
		var isMusicEnabled = false
			private set

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
			} else if (Simulation.isStarted) {
				MUSIC_GAME.loop()
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
