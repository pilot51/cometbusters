/*
 * Copyright 2016-2023 Mark Injerd
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

import MultiplayerManager.Companion.instance as mpMan

object ShipManager {
	val localShip by lazy { Ship() }
	private val MP_SPAWN_POSITIONS = arrayOf(
		Entity.Position((GameView.VIEW_WIDTH / 4).toFloat(), (GameView.VIEW_HEIGHT / 4).toFloat()),
		Entity.Position((GameView.VIEW_WIDTH * 3 / 4).toFloat(), (GameView.VIEW_HEIGHT * 3 / 4).toFloat()),
		Entity.Position((GameView.VIEW_WIDTH * 3 / 4).toFloat(), (GameView.VIEW_HEIGHT / 4).toFloat()),
		Entity.Position((GameView.VIEW_WIDTH / 4).toFloat(), (GameView.VIEW_HEIGHT * 3 / 4).toFloat())
	)
	val ships = ArrayList<Ship?>(4)
		get() {
			if (field.isEmpty()) {
				field.add(localShip)
				localShip.setPlayerColor(0)
			}
			return field
		}

	fun addShip(ship: Ship, id: Int) {
		if (id >= ships.size) {
			while (id > ships.size) {
				ships.add(null)
			}
			ships.add(ship)
		} else {
			ships[id] = ship
		}
		ship.setPlayerColor(id)
	}

	fun removeShip(id: Int) {
		ships[id] = null
	}

	fun clearShips() {
		ships.clear()
		localShip.setPlayerColor(0)
	}

	fun getPlayerId(ship: Ship?) = ships.indexOf(ship)

	fun getSpawnPosition(playerId: Int): Entity.Position {
		return if (mpMan.isConnected) {
			MP_SPAWN_POSITIONS[playerId]
		} else {
			Entity.Position((GameView.VIEW_WIDTH / 2).toFloat(), (GameView.VIEW_HEIGHT / 2).toFloat())
		}
	}
}
