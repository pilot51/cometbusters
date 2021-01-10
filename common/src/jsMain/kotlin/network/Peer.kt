/*
 * Copyright 2021 Mark Injerd
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

package network

import kotlinext.js.jsObject

/** [Original source](https://github.com/ColoredCarrot/poker-game/blob/38c8ee8b/src/main/kotlin/comm/Peer.kt) */
class Peer {
	private val peer = PeerJS.createPeer()
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

		peer.on("error") { err: dynamic ->
			log("Connection error: ", err)
			hook.error?.let { it(err) }
		}
	}

	fun connect(theirId: String) {
		@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "RemoveExplicitTypeArguments")
		val them = peer.connect(theirId, jsObject<dynamic> {
			reliable = false
			serialization = "none"
		}) as DataConnection
		remotes[theirId] = them
		them.on("error") { err: dynamic ->
			log("error connecting to $theirId: ", err)
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

		remote.on("open", fun() {
			hook.connected?.let { it(remoteId) }
		})

		remote.on("data", fun(data: dynamic) {
			hook.receiveData?.let { it(data as String) }
		})

		remote.on("close", fun() {
			hook.disconnected?.let { it(remoteId) }
			remotes.remove(remoteId)
		})
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

	private fun log(msg: String, vararg more: dynamic) = console.log("[Peer] $msg", *more)

	class Hook {
		internal var open: ((String) -> Unit)? = null
		internal var close: (() -> Unit)? = null
		internal var error: ((dynamic) -> Unit)? = null
		internal var connected: ((String) -> Unit)? = null
		internal var disconnected: ((String) -> Unit)? = null
		internal var errConnecting: ((dynamic) -> Unit)? = null
		internal var receiveData: ((String) -> Unit)? = null

		fun onOpen(handler: ((peerId: String) -> Unit)?) = run { open = handler }

		fun onClose(handler: (() -> Unit)?) = run { close = handler }

		fun onError(handler: ((err: dynamic) -> Unit)?) = run { error = handler }

		fun onConnected(handler: ((peerId: String) -> Unit)?) = run { connected = handler }

		fun onDisconnected(handler: ((peerId: String) -> Unit)?) = run { disconnected = handler }

		fun onErrorConnecting(handler: ((err: dynamic) -> Unit)?) = run { errConnecting = handler }

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
}

private external interface DataConnection {
	val peer: String
	val open: Boolean
	fun close()
	fun send(data: dynamic)
	fun on(event: String, callback: dynamic)
}
