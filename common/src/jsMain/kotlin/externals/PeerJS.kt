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

external class Peer(id: String?, options: PeerOptions?) {
	fun connect(peer: String, options: PeerConnectOption): DataConnection
	fun destroy()
	fun on(event: String, callback: Function<Unit>)
}

external class PeerOptions {
	/** 1: Errors, 2: Warnings, 3: All logs */
	var debug: Int?
}

external class PeerConnectOption {
	var reliable: Boolean
	var serialization: String
}

external interface DataConnection {
	val peer: String
	val open: Boolean
	fun close()
	fun send(data: Any)
	fun on(event: String, callback: Function<Unit>)
}
