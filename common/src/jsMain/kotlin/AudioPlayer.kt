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

import externals.AudioBuffer
import externals.AudioBufferSourceNode
import externals.AudioContext
import externals.MIDIjs
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.w3c.fetch.CORS
import org.w3c.fetch.RequestInit
import org.w3c.fetch.RequestMode

actual class AudioPlayer actual constructor(
	private val audio: Audio,
	private val doLoop: Boolean
) {
	private var srcNode: AudioBufferSourceNode? = null

	actual fun start() {
		if (audio.isMusic) {
			MIDIjs.play(audio.filename, true)
		} else {
			getAudio { play(it) }
		}
	}

	actual fun stop() {
		if (audio.isMusic) {
			MIDIjs.stop()
		} else {
			srcNode?.stop()
		}
	}

	private fun play(audioBuffer: AudioBuffer) {
		srcNode = audioContext.createBufferSource().apply {
			buffer = audioBuffer
			loop = doLoop
			connect(audioContext.destination)
			start()
		}
	}

	private fun getAudio(callback: (AudioBuffer) -> Unit) =
		audioCache[audio]?.let(callback) ?: run {
			window.fetch(audio.filename, RequestInit(mode = RequestMode.CORS)).then {
				it.arrayBuffer()
			}.then {
				CoroutineScope(Dispatchers.Default).launch {
					val buffer = audioContext.decodeAudioData(it).await()
					audioCache[audio] = buffer
					callback(buffer)
				}
			}
		}

	companion object {
		private val audioContext = AudioContext()
		private val audioCache = mutableMapOf<Audio, AudioBuffer>()
	}
}
