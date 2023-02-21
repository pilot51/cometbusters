/*
 * Copyright 2021-2023 Mark Injerd
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

import externals.DataConnection
import externals.Peer
import externals.PeerConnectOption
import externals.PeerJSOption

/** [Original source](https://github.com/ColoredCarrot/poker-game/blob/38c8ee8b/src/main/kotlin/comm/network.Peer.kt) */
class PeerHelper {
	private val peer = createPeer()
	private val remotes = LinkedHashMap<String, DataConnection>(3)
	val hook = Hook()

	init {
		peer.on("open") { id: String ->
			log("Connected to signaling server. Assigned ID: $id")
			hook.open?.let { it(id) }
		}

		peer.on("disconnected") {
			log("Disconnected from server")
			hook.close?.let { it() }
		}

		peer.on("connection") { remote: DataConnection ->
			remotes[remote.peer] = remote
			ready(remote)
		}

		peer.on("close") {
			log("Connection closed")
			remotes.clear()
		}

		peer.on("error") { err: Error ->
			log("Connection error: ", err.message)
			hook.error?.let { it(err) }
		}
	}

	fun connect(theirId: String) {
		val them = peer.connect(theirId, PEERJS_CONNECT_OPTS)
		remotes[theirId] = them
		them.on("error") { err: Error ->
			log("error connecting to $theirId: ", err.message)
			hook.errConnecting?.let { it(err) }
		}
		ready(them)
	}

	fun disconnect(theirId: String) {
		remotes[theirId]?.close()
	}

	fun disconnect() {
		peer.destroy()
		hook.clear()
	}

	private fun ready(remote: DataConnection) {
		val remoteId = remote.peer
		remote.on("open") {
			hook.connected?.let { it(remoteId) }
		}
		remote.on("data") { data: String ->
			hook.receiveData?.let { it(data) }
		}
		remote.on("close") {
			hook.disconnected?.let { it(remoteId) }
			remotes.remove(remoteId)
		}
	}

	fun send(sessionId: String, data: String) {
		val dataCreator: (peer: String) -> String = { data }
		val remote = remotes[sessionId]
		if (remote?.open == true) {
			remote.send(dataCreator(sessionId))
		} else {
			console.warn("Skipping sending message to $peer because the connection is closed.")
		}
	}

	private fun log(msg: String, vararg more: Any?) = console.log("[network.Peer] $msg", *more)

	class Hook {
		internal var open: ((String) -> Unit)? = null
		internal var close: (() -> Unit)? = null
		internal var error: ((Error) -> Unit)? = null
		internal var connected: ((String) -> Unit)? = null
		internal var disconnected: ((String) -> Unit)? = null
		internal var errConnecting: ((Error) -> Unit)? = null
		internal var receiveData: ((String) -> Unit)? = null

		fun onOpen(handler: ((peerId: String) -> Unit)?) = run { open = handler }
		fun onClose(handler: (() -> Unit)?) = run { close = handler }
		fun onError(handler: ((err: Error) -> Unit)?) = run { error = handler }
		fun onConnected(handler: ((peerId: String) -> Unit)?) = run { connected = handler }
		fun onDisconnected(handler: ((peerId: String) -> Unit)?) = run { disconnected = handler }
		fun onErrorConnecting(handler: ((err: Error) -> Unit)?) = run { errConnecting = handler }
		fun onReceiveData(handler: ((data: String) -> Unit)?) = run { receiveData = handler }

		internal fun clear() {
			open = null
			close = null
			error = null
			connected = null
			disconnected = null
			errConnecting = null
			receiveData = null
		}
	}

	companion object {
		private val PEERJS_CLOUD_INIT = object : PeerJSOption {
			override var debug: Int? = 2
		}
		private val PEERJS_CONNECT_OPTS = object : PeerConnectOption {
			override var reliable = false
			override var serialization = "none"
		}

		private fun createPeer() = Peer(null, PEERJS_CLOUD_INIT)
	}
}
