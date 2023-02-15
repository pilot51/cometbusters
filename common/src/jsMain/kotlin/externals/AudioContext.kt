/*
 * Copyright 2023 Mark Injerd
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

package externals

import org.khronos.webgl.ArrayBuffer
import kotlin.js.Promise

external class AudioContext {
	val destination: AudioNode
	fun close()
	fun createBufferSource(): AudioBufferSourceNode
	fun decodeAudioData(
		data: ArrayBuffer,
		successCallback: ((AudioBuffer) -> Unit)? = definedExternally,
		errorCallback: (() -> Unit)? = definedExternally
	): Promise<AudioBuffer>
}

open external class AudioNode {
	fun connect(
		destination: AudioNode,
		output: Int = definedExternally,
		input: Int = definedExternally
	): AudioNode
}

external class AudioBuffer

external class AudioBufferSourceNode : AudioNode {
	fun start(time: Double = definedExternally)
	fun stop(time: Double = definedExternally)
	var buffer: AudioBuffer
	var loop: Boolean
}
