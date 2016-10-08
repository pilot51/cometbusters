/*
 * Copyright 2016 Mark Injerd
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

import java.util.ArrayList;

public class ShipManager {
	private static Ship localShip;
	private static final ArrayList<Ship> SHIPS = new ArrayList<Ship>(4);;
	private static final Entity.Position[] MP_SPAWN_POSITIONS = new Entity.Position[] {
			new Entity.Position(GameView.VIEW_WIDTH / 4, GameView.VIEW_HEIGHT / 4),
			new Entity.Position(GameView.VIEW_WIDTH * 3/4, GameView.VIEW_HEIGHT * 3/4),
			new Entity.Position(GameView.VIEW_WIDTH * 3/4, GameView.VIEW_HEIGHT / 4),
			new Entity.Position(GameView.VIEW_WIDTH / 4, GameView.VIEW_HEIGHT * 3/4)};

	static Ship getLocalShip() {
		if (localShip == null) {
			localShip = new Ship();
		}
		return localShip;
	}

	static ArrayList<Ship> getShips() {
		if (SHIPS.isEmpty()) {
			addShip(getLocalShip(), 0);
		}
		return SHIPS;
	}

	static void addShip(Ship ship, int id) {
		synchronized (SHIPS) {
			if (id >= SHIPS.size()) {
				while (id > SHIPS.size()) {
					SHIPS.add(null);
				}
				SHIPS.add(ship);
			} else {
				SHIPS.set(id, ship);
			}
		}
		ship.setPlayerColor(id);
	}

	/**
	 * @param ship The ship to remove.
	 * @return The player ID that was removed.
	 */
	static void removeShip(int id) {
		synchronized (SHIPS) {
			SHIPS.set(id, null);
		}
	}

	static void clearShips() {
		synchronized (SHIPS) {
			SHIPS.clear();
		}
		localShip.setPlayerColor(0);
	}

	static int getPlayerId(Ship ship) {
		return SHIPS.indexOf(ship);
	}

	static Entity.Position getSpawnPosition(int playerId) {
		if (MultiplayerManager.getInstance().isConnected()) {
			return MP_SPAWN_POSITIONS[playerId];
		} else {
			return new Entity.Position(GameView.VIEW_WIDTH / 2, GameView.VIEW_HEIGHT / 2);
		}
	}
}
