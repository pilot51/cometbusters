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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class MultiplayerManager {
	private static MultiplayerManager instance;
	private static final int MESSAGE_TYPE_GAME = 0, MESSAGE_TYPE_LEVEL = 1, MESSAGE_TYPE_ASTEROIDS = 2, MESSAGE_TYPE_PLAYER = 3,
			MESSAGE_TYPE_BULLET = 4, MESSAGE_TYPE = 0, IS_STARTED = 1, IS_PAUSED = 2, LEVEL = 1, PLAYER_IS_CONNECTED = 2,
			ASTEROIDS_DATA_SIZE = 5, ASTEROID_POSX = 1, ASTEROID_POSY = 2, ASTEROID_DIR = 3, ASTEROID_VEL = 4, ASTEROID_SIZE = 5, 
			PLAYER_ID = 1, POSX = 2, POSY = 3, DIRECTION = 4, ACCEL = 5, DESTROYED = 6, SCORE = 7, LIVES = 8,
			SHIP_BULLET_DATA_SIZE = 3, SHIP_BULLET_POSX = 9, SHIP_BULLET_POSY = 10, SHIP_BULLET_DIR = 11;
	private List<Connection> connections = new ArrayList<Connection>();
	private ServerSocket serverSocket;
	private static final int PORT = 50001;
	private boolean isConnected, isClient;
	private ConnectionStateListener connectionListener;
	private Simulation.GameStateListener gameStateListener = new Simulation.GameStateListener() {
		@Override
		public void onGameStartStateChanged(boolean started) {
			sendGameData();
		}

		@Override
		public void onGamePauseStateChanged(boolean paused) {
			sendGameData();
		}
	};

	interface ConnectionStateListener {
		void onHostWaiting();
		void onConnected();
		void onDisconnected();
	}

	private MultiplayerManager() {
		Simulation.addGameStateListener(gameStateListener);
	}

	static MultiplayerManager getInstance() {
		if (instance == null) {
			instance = new MultiplayerManager();
		}
		return instance;
	}

	void setConnectionStateListener(ConnectionStateListener listener) {
		connectionListener = listener;
	}

	private void startListening(Connection conn) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (connections.contains(conn)) {
					String msg = receive(conn);
					if (msg != null) {
						String[] data = msg.split(" ");
						switch (Integer.parseInt(data[MESSAGE_TYPE])) {
						case MESSAGE_TYPE_GAME:
							onReceiveGameData(Boolean.parseBoolean(data[IS_STARTED]), Boolean.parseBoolean(data[IS_PAUSED]));
							break;
						case MESSAGE_TYPE_LEVEL:
							onReceiveLevel(Integer.parseInt(data[LEVEL]));
							break;
						case MESSAGE_TYPE_ASTEROIDS:
							List<Asteroid> asteroids = null;
							if (data.length > ASTEROID_POSX) {
								asteroids = new ArrayList<Asteroid>();
								for (int i = 0; i*ASTEROIDS_DATA_SIZE+ASTEROID_POSX < data.length; i++) {
									asteroids.add(new Asteroid(Float.parseFloat(data[i*ASTEROIDS_DATA_SIZE+ASTEROID_POSX]),
											Float.parseFloat(data[i*ASTEROIDS_DATA_SIZE+ASTEROID_POSY]),
											Integer.parseInt(data[i*ASTEROIDS_DATA_SIZE+ASTEROID_DIR]),
											Integer.parseInt(data[i*ASTEROIDS_DATA_SIZE+ASTEROID_VEL]),
											Asteroid.Size.valueOf(data[i*ASTEROIDS_DATA_SIZE+ASTEROID_SIZE])));
								}
							}
							onReceiveAsteroidData(asteroids);
							break;
						case MESSAGE_TYPE_PLAYER:
							if (data.length == 3) {
								onReceivePlayerConnectionStatus(Integer.parseInt(data[PLAYER_ID]), Boolean.parseBoolean(data[PLAYER_IS_CONNECTED]));
							} else {
								List<Bullet> bullets = null;
								if (data.length > SHIP_BULLET_POSX) {
									bullets = new ArrayList<Bullet>();
									for (int i = 0; i*SHIP_BULLET_DATA_SIZE+SHIP_BULLET_POSX < data.length; i++) {
										bullets.add(new Bullet(Integer.parseInt(data[PLAYER_ID]),
												Float.parseFloat(data[i*SHIP_BULLET_DATA_SIZE+SHIP_BULLET_POSX]),
												Float.parseFloat(data[i*SHIP_BULLET_DATA_SIZE+SHIP_BULLET_POSY]),
												Integer.parseInt(data[i*SHIP_BULLET_DATA_SIZE+SHIP_BULLET_DIR])));
									}
								}
								onReceivePlayerData(conn, Integer.parseInt(data[PLAYER_ID]), Float.parseFloat(data[POSX]),
										Float.parseFloat(data[POSY]), Integer.parseInt(data[DIRECTION]), Boolean.parseBoolean(data[ACCEL]),
										Boolean.parseBoolean(data[DESTROYED]), Integer.parseInt(data[SCORE]), Integer.parseInt(data[LIVES]), bullets);
							}
							break;
						case MESSAGE_TYPE_BULLET:
							onReceiveFiredBulletData(conn, new Bullet(Integer.parseInt(data[PLAYER_ID]), Float.parseFloat(data[POSX]),
									Float.parseFloat(data[POSY]), Integer.parseInt(data[DIRECTION])));
							break;
						}
					} else {
						disconnect(conn);
						break;
					}
				};
			}
		}).start();
	}

	void startHost() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("Starting host on port " + PORT);
				try {
					serverSocket = new ServerSocket(PORT);
				} catch (IOException e) {
					System.out.println("Could not listen on port " + PORT);
					return;
				}
				connectionListener.onHostWaiting();
				while (serverSocket != null) {
					Connection conn = new Connection();
					try {
						conn.clientSocket = serverSocket.accept();
						conn.ipRemote = conn.clientSocket.getInetAddress();
						System.out.println("Incoming connection: " + conn.ipRemote.getHostAddress() + ":" + conn.clientSocket.getPort());
						conn.in = new BufferedReader(new InputStreamReader(conn.clientSocket.getInputStream()));
						conn.out = new PrintWriter(conn.clientSocket.getOutputStream(), true);
						connections.add(conn);
						isClient = false;
						onConnected(conn);
						startListening(conn);
					} catch (IOException e) {
						if (serverSocket != null) {
							System.out.println("Connection failed!");
							e.printStackTrace();
							disconnect(conn);
						}
					}
					if (connections.size() == 3) {
						System.out.println("Connections full - waiting for a connection to drop before listening for new connections.");
						synchronized (connections) {
							try {
								connections.wait();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						System.out.println("Connection dropped - resume listening for new connections.");
					}
				}
			}
		}).start();
	}

	void connect(String address) {
		if (address == null || address.isEmpty()) {
			return;
		}
		Connection conn = new Connection();
		try {
			conn.ipRemote = InetAddress.getByName(address);
			new Thread(new Runnable() {
				@Override
				public void run() {
					System.out.println("Connecting to server: " + conn.ipRemote.getHostAddress() + ":" + PORT);
					try {
						conn.clientSocket = new Socket(conn.ipRemote, PORT);
						System.out.println("Local client port: " + conn.clientSocket.getLocalPort());
						conn.out = new PrintWriter(conn.clientSocket.getOutputStream(), true);
						conn.in = new BufferedReader(new InputStreamReader(conn.clientSocket.getInputStream()));
						connections.add(conn);
						isClient = true;
						onConnected(conn);
						startListening(conn);
					} catch (IOException e) {
						System.out.println("Connection failed!");
						e.printStackTrace();
						disconnect(conn);
					}
				}
			}).start();
		} catch (UnknownHostException e) {
			System.out.println("Unknown host: " + address);
		}
	}

	/** Disconnects all connections. */
	void disconnect() {
		for (int i = connections.size() - 1; i >= 0; i--) {
			disconnect(connections.get(i));
		}
	}

	/**
	 * Disconnects one connection.
	 * @param conn The connection to disconnect.
	 */
	private void disconnect(Connection conn) {
		if (!connections.remove(conn)) return;
		synchronized (connections) {
			connections.notify();
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (conn.clientSocket != null) {
					try {
						conn.clientSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println("Disconnected from " + conn.ipRemote.getHostAddress() + ":" + PORT
							+ " (playerId " + ShipManager.getPlayerId(conn.remoteShip) + ")");
				}
				if (connections.isEmpty() && serverSocket != null) {
					try {
						serverSocket.close();
						serverSocket = null;
						System.out.println("Stopped host");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				onDisconnected(conn);
			}
		}).start();
	}

	private void sendGameData() {
		if (connections.isEmpty() || isClient) return;
		String[] data = new String[3];
		data[MESSAGE_TYPE] = Integer.toString(MESSAGE_TYPE_GAME);
		data[IS_STARTED] = Boolean.toString(Simulation.isStarted());
		data[IS_PAUSED] = Boolean.toString(Simulation.isPaused());
		send(data);
	}

	void sendLevel() {
		if (connections.isEmpty() || isClient) return;
		String[] data = new String[3];
		data[MESSAGE_TYPE] = Integer.toString(MESSAGE_TYPE_LEVEL);
		data[LEVEL] = Integer.toString(LevelManager.getLevel());
		send(data);
	}

	/** Sends all data for all asteroids. */
	void sendAsteroids() {
		if (connections.isEmpty() || isClient) return;
		String[] data = new String[1 + 5 * Asteroid.getAsteroids().size()];
		data[MESSAGE_TYPE] = Integer.toString(MESSAGE_TYPE_ASTEROIDS);
		synchronized (Asteroid.getAsteroids()) {
			for (int i = 0; i < Asteroid.getAsteroids().size(); i++) {
				Asteroid a = Asteroid.getAsteroids().get(i);
				data[i*ASTEROIDS_DATA_SIZE+ASTEROID_POSX] = Float.toString(a.pos.x);
				data[i*ASTEROIDS_DATA_SIZE+ASTEROID_POSY] = Float.toString(a.pos.y);
				data[i*ASTEROIDS_DATA_SIZE+ASTEROID_DIR] = Integer.toString(a.direction);
				data[i*ASTEROIDS_DATA_SIZE+ASTEROID_VEL] = Integer.toString(a.velocity);
				data[i*ASTEROIDS_DATA_SIZE+ASTEROID_SIZE] = a.getSize().name();
			}
		}
		send(data);
	}

	/**
	 * Sends data for the specified ship, including its bullets.
	 * @param ship The ship to send.
	 */
	private void sendShipData(Ship ship) {
		if (connections.isEmpty()) return;
		synchronized (ship.getBullets()) {
			String[] data = new String[9 + 3 * ship.getBullets().size()];
			data[MESSAGE_TYPE] = Integer.toString(MESSAGE_TYPE_PLAYER);
			data[PLAYER_ID] = Integer.toString(ShipManager.getShips().indexOf(ship));
			data[POSX] = Float.toString(ship.pos.x);
			data[POSY] = Float.toString(ship.pos.y);
			data[DIRECTION] = Integer.toString(ship.rotateDeg);
			data[ACCEL] = Boolean.toString(ship.isAccelerating);
			data[DESTROYED] = Boolean.toString(ship.isDestroyed());
			data[SCORE] = Integer.toString(ship.getScore());
			data[LIVES] = Integer.toString(ship.getLives());
			for (int i = 0; i < ship.getBullets().size(); i++) {
				Bullet b = ship.getBullets().get(i);
				data[i*SHIP_BULLET_DATA_SIZE+SHIP_BULLET_POSX] = Float.toString(b.pos.x);
				data[i*SHIP_BULLET_DATA_SIZE+SHIP_BULLET_POSY] = Float.toString(b.pos.y);
				data[i*SHIP_BULLET_DATA_SIZE+SHIP_BULLET_DIR] = Integer.toString(b.direction);
			}
			send(data);
		}
	}

	/**
	 * Sends the connection status of a player.
	 * @param playerId The ID of the player to be sent.
	 * @param connected True if the player is connected, false if not.
	 */
	private void sendPlayerConnectionStatus(int playerId, boolean connected) {
		if (connections.isEmpty() || isClient) return;
		String[] data = new String[3];
		data[MESSAGE_TYPE] = Integer.toString(MESSAGE_TYPE_PLAYER);
		data[PLAYER_ID] = Integer.toString(playerId);
		data[PLAYER_IS_CONNECTED] = Boolean.toString(connected);
		for (Connection conn : connections) {
			if (playerId != ShipManager.getPlayerId(conn.remoteShip)) {
				send(conn, data);
			}
		}
	}

	/**
	 * Sends the connection status of all connected players (minus the target player) to one client.
	 * @param conn The connection to receive the connection data.
	 */
	private void sendConnectedPlayers(Connection conn) {
		if (connections.isEmpty() || isClient) return;
		for (Connection otherConn : connections) {
			if (otherConn == conn) continue;
			String[] data = new String[3];
			data[MESSAGE_TYPE] = Integer.toString(MESSAGE_TYPE_PLAYER);
			data[PLAYER_ID] = Integer.toString(ShipManager.getPlayerId(otherConn.remoteShip));
			data[PLAYER_IS_CONNECTED] = Boolean.toString(true);
			send(conn, data);
		}
	}

	/**
	 * Client: Sends data for the local ship.
	 * Server: Sends data for all ships.
	 */
	void sendShipUpdate() {
		if (connections.isEmpty()) return;
		if (isClient && !ShipManager.getLocalShip().isDestroyed()) {
			sendShipData(ShipManager.getLocalShip());
		} else {
			List<Ship> ships = ShipManager.getShips();
			synchronized (ships) {
				for (Ship s : ships) {
					if (s == null) continue;
					sendShipData(s);
				}
			}
		}
	}

	/**
	 * Sends the initial position and direction of a single bullet along with the ID of the player who shot it.
	 * If host, sends to all clients except the originating client. Clients only send to host.
	 * @param bullet The fired bullet.
	 */
	void sendFiredBullet(Bullet bullet) {
		if (connections.isEmpty()) return;
		String[] data = new String[5];
		data[MESSAGE_TYPE] = Integer.toString(MESSAGE_TYPE_BULLET);
		data[PLAYER_ID] = Integer.toString(bullet.getPlayerId());
		data[POSX] = Float.toString(bullet.pos.x);
		data[POSY] = Float.toString(bullet.pos.y);
		data[DIRECTION] = Integer.toString(bullet.direction);
		for (Connection conn : connections) {
			if (bullet.getPlayerId() != ShipManager.getPlayerId(conn.remoteShip)) {
				send(conn, data);
			}
		}
	}

	/**
	 * Sends data to all connections.
	 * @param data The data to be sent.
	 */
	private void send(String[] data) {
		for (Connection conn : connections) {
			if (conn.out == null) {
				return;
			}
			conn.out.println(String.join(" ", data));
		}
	}

	/**
	 * Sends data to one connection.
	 * @param conn The connection to send the data to.
	 * @param data The data to be sent.
	 */
	private void send(Connection conn, String[] data) {
		if (conn.out == null) {
			return;
		}
		conn.out.println(String.join(" ", data));
	}

	/**
	 * Receives data from a connection.
	 * @param conn The connection to receive the data from.
	 */
	private String receive(Connection conn) {
		if (conn.in == null) {
			return null;
		}
		String msg = null;
		try {
			msg = conn.in.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return msg;
	}

	boolean isConnected() {
		return isConnected;
	}

	boolean isClient() {
		return isClient;
	}

	/**
	 * Called when a connection has been established to do some game setup for the new player.
	 * @param conn The new connection.
	 */
	private void onConnected(Connection conn) {
		System.out.println("Connected!");
		isConnected = true;
		connectionListener.onConnected();
		if (isClient) {
			conn.remoteShip = new Ship();
			ShipManager.addShip(conn.remoteShip, 0);
			int localPlayerId = Integer.parseInt(receive(conn));
			System.out.println("Local player ID: " + localPlayerId);
			ShipManager.addShip(ShipManager.getLocalShip(), localPlayerId);
		} else {
			int remotePlayerId = ShipManager.getPlayerId(null);
			if (remotePlayerId == -1) {
				remotePlayerId = ShipManager.getShips().size();
			}
			conn.out.println(remotePlayerId);
			sendGameData();
			sendAsteroids();
			ShipManager.addShip(ShipManager.getLocalShip(), 0);
			conn.remoteShip = new Ship();
			if (Simulation.isStarted()) {
				conn.remoteShip.setLives(0);
			}
			ShipManager.addShip(conn.remoteShip, remotePlayerId);
			System.out.println("Remote client playerId: " + ShipManager.getPlayerId(conn.remoteShip));
			sendConnectedPlayers(conn);
			sendPlayerConnectionStatus(ShipManager.getPlayerId(conn.remoteShip), true);
		}
	}

	/**
	 * Called when a connection has been dropped.
	 * @param conn The dropped connection.
	 */
	private void onDisconnected(Connection conn) {
		int playerId = ShipManager.getPlayerId(conn.remoteShip);
		sendPlayerConnectionStatus(playerId, false);
		ShipManager.removeShip(playerId);
		if (connections.isEmpty()) {
			Simulation.setStarted(false);
			isConnected = false;
			isClient = false;
			connectionListener.onDisconnected();
			ShipManager.clearShips();
		}
	}

	private void onReceiveGameData(boolean isStarted, boolean isPaused) {
		Simulation.setStarted(isStarted);
		Simulation.setPaused(isPaused);
	}

	private void onReceiveLevel(int level) {
		LevelManager.startLevel(level);
	}

	/**
	 * Called when a player connection status has been received.
	 * @param playerId The player ID for the status.
	 * @param connected True if the player is connected, false if not.
	 */
	private void onReceivePlayerConnectionStatus(int playerId, boolean connected) {
		System.out.println("Receive - Player " + (connected ? "connected" : "disconnected") + ": " + playerId);
		if (connected) {
			ShipManager.addShip(new Ship(), playerId);
		} else {
			ShipManager.removeShip(playerId);
		}
	}

	/**
	 * Called when player game data has been received.
	 * @param conn The connection that the data has been received from. Only used by the host.
	 * @param playerId The player ID the data belongs to.
	 * @param posX The ship x position.
	 * @param posY The ship y position.
	 * @param rotationDeg The ship rotation in degrees.
	 * @param thrust True if the ship is thrusting, false if not.
	 * @param isDestroyed True if the ship is destroyed, false if alive.
	 * @param score The score for the player.
	 * @param lives The number of lives the player has.
	 * @param bullets List of active bullets that the player has shot.
	 */
	private void onReceivePlayerData(Connection conn, int playerId, float posX, float posY, int rotationDeg, boolean thrust,
			boolean isDestroyed, int score, int lives, List<Bullet> bullets) {
		List<Ship> ships = ShipManager.getShips();
		if (!isClient && playerId != 0) {
			conn.remoteShip.forceUpdate(posX, posY, rotationDeg, thrust);
		} else if (isClient) {
			if (playerId >= ships.size()) {
				System.out.println("Received data for uninitialized player");
				return;
			}
			Ship ship = ships.get(playerId);
			if (ship == null) {
				System.out.println("Received data for null player");
				return;
			}
			ship.setScore(score);
			ship.setLives(lives);
			if (ship != ShipManager.getLocalShip()) {
				ship.forceUpdate(posX, posY, rotationDeg, thrust);
			}
			synchronized (ship.getBullets()) {
				ship.getBullets().clear();
				if (bullets != null) {
					ship.getBullets().addAll(bullets);
				}
			}
			if (isDestroyed && !ship.isDestroyed()) {
				ship.destroy();
			} else if (!isDestroyed && ship.isDestroyed()) {
				ship.spawn();
			}
		}
	}

	/**
	 * Called when data for a fired bullet is received.
	 * If host, it is forwarded to other clients and added to the player. If client, only the shoot sound is played.
	 * @param conn The connection where the shot originated from, only used by host.
	 * @param bullet The fired bullet.
	 */
	private void onReceiveFiredBulletData(Connection conn, Bullet bullet) {
		if (!isClient) {
			sendFiredBullet(bullet);
			List<Bullet> bullets = conn.remoteShip.getBullets();
			synchronized (bullets) {
				bullets.add(bullet);
			}
		}
		Audio.SHOOT.play();
	}

	/**
	 * Called when asteroid data is received.
	 * @param updatedAsteroids List of asteroids.
	 */
	private void onReceiveAsteroidData(List<Asteroid> updatedAsteroids) {
		List<Asteroid> asteroids = Asteroid.getAsteroids();
		synchronized (asteroids) {
			if (updatedAsteroids == null) {
				if (asteroids.size() == 1 && asteroids.get(0).getSize() == Asteroid.Size.SMALL) {
					Audio.EXPLODE_SMALL.play();
				}
				asteroids.clear();
			} else {
				for (int i = Math.min(asteroids.size(),  updatedAsteroids.size());
						i < Math.max(asteroids.size(),  updatedAsteroids.size()); i++) {
					if (i >= asteroids.size()) {
						switch (updatedAsteroids.get(i).getSize()) {
						case LARGE:
							break;
						case MEDIUM:
							Audio.EXPLODE_LARGE.play();
							break;
						case SMALL:
							Audio.EXPLODE_MEDIUM.play();
							break;
						}
					} else if (i >= updatedAsteroids.size()) {
						Audio.EXPLODE_SMALL.play();
					}
				}
				asteroids.clear();
				asteroids.addAll(updatedAsteroids);
			}
		}
	}

	private class Connection {
		private Socket clientSocket;
		private PrintWriter out;
		private BufferedReader in;
		private InetAddress ipRemote;
		private Ship remoteShip;
	}
}
