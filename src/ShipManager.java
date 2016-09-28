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

import java.io.IOException;
import java.util.ArrayList;

public class ShipManager {
	private static Ship localShip;
	private static ArrayList<Ship> ships;
	private static final Entity.Position[] MP_SPAWN_POSITIONS = new Entity.Position[] {
			new Entity.Position(GameView.VIEW_WIDTH / 4, GameView.VIEW_HEIGHT / 4),
			new Entity.Position(GameView.VIEW_WIDTH * 3/4, GameView.VIEW_HEIGHT * 3/4),
			new Entity.Position(GameView.VIEW_WIDTH * 3/4, GameView.VIEW_HEIGHT / 4),
			new Entity.Position(GameView.VIEW_WIDTH / 4, GameView.VIEW_HEIGHT * 3/4)};

	static Ship getLocalShip() {
		if (localShip == null) {
			try {
				localShip = new Ship();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return localShip;
	}

	static ArrayList<Ship> getShips() {
		if (ships == null) {
			ships = new ArrayList<Ship>(4);
			ships.add(getLocalShip());
		}
		return ships;
	}

	void addShip(Ship ship, int id) {
		if (id == ships.size()) {
			ships.add(ship);
		} else {
			ships.set(id, ship);
		}
	}

	static void clearShips() {
		ships = null;
	}

	static int getPlayerId(Ship ship) {
		return ships.indexOf(ship);
	}

	static Entity.Position getSpawnPosition(int playerId) {
		if (MultiplayerManager.getInstance().isConnected()) {
			return MP_SPAWN_POSITIONS[playerId];
		} else {
			return new Entity.Position(GameView.VIEW_WIDTH / 2, GameView.VIEW_HEIGHT / 2);
		}
	}
}
