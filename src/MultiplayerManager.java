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
	private static final int MESSAGE_TYPE_GAME = 0, MESSAGE_TYPE_LEVEL = 1, MESSAGE_TYPE_ASTEROIDS = 2,
			MESSAGE_TYPE_PLAYER = 3, MESSAGE_TYPE_BULLET = 4, MESSAGE_TYPE = 0, IS_STARTED = 1, IS_PAUSED = 2,
			ASTEROIDS_DATA_SIZE = 5, ASTEROID_POSX = 1, ASTEROID_POSY = 2, ASTEROID_DIR = 3, ASTEROID_VEL = 4, ASTEROID_SIZE = 5, 
			PLAYER_ID = 1, POSX = 2, POSY = 3, DIRECTION = 4, ACCEL = 5, DESTROYED = 6, SCORE = 7, LIVES = 8,
			SHIP_BULLET_DATA_SIZE = 3, SHIP_BULLET_POSX = 9, SHIP_BULLET_POSY = 10, SHIP_BULLET_DIR = 11;
	private Socket clientSocket;
	private ServerSocket serverSocket;
	private PrintWriter out;
	private BufferedReader in;
	private InetAddress ipRemote;
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

	private void startListening() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (clientSocket != null && !clientSocket.isClosed()) {
					String msg = receive();
					if (msg != null) {
						String[] data = msg.split(" ");
						switch (Integer.parseInt(data[MESSAGE_TYPE])) {
						case MESSAGE_TYPE_GAME:
							onReceiveGameData(Boolean.parseBoolean(data[IS_STARTED]), Boolean.parseBoolean(data[IS_PAUSED]));
							break;
						case MESSAGE_TYPE_LEVEL:
							onReceiveLevel(Integer.parseInt(data[1]));
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
							onReceivePlayerData(Integer.parseInt(data[PLAYER_ID]), Float.parseFloat(data[POSX]),
									Float.parseFloat(data[POSY]), Integer.parseInt(data[DIRECTION]), Boolean.parseBoolean(data[ACCEL]),
									Boolean.parseBoolean(data[DESTROYED]), Integer.parseInt(data[SCORE]), Integer.parseInt(data[LIVES]), bullets);
							break;
						case MESSAGE_TYPE_BULLET:
							onReceiveFiredBulletData(new Bullet(Integer.parseInt(data[PLAYER_ID]), Float.parseFloat(data[POSX]),
									Float.parseFloat(data[POSY]), Integer.parseInt(data[DIRECTION])));
							break;
						}
					} else {
						disconnect();
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
				try{
					clientSocket = serverSocket.accept();
					ipRemote = clientSocket.getInetAddress();
					System.out.println("Incoming connection: " + ipRemote.getHostAddress() + ":" + clientSocket.getPort());
					in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
					out = new PrintWriter(clientSocket.getOutputStream(), true);
					onConnected(0);
					startListening();
				} catch (IOException e) {
					if (serverSocket != null) {
						System.out.println("Connection failed!");
						e.printStackTrace();
						disconnect();
					}
				}
			}
		}).start();
	}

	void connect(String address) {
		if (address == null || address.isEmpty()) {
			return;
		}
		try {
			ipRemote = InetAddress.getByName(address);
			new Thread(new Runnable() {
				@Override
				public void run() {
					System.out.println("Connecting to server: " + ipRemote.getHostAddress() + ":" + PORT);
					try {
						clientSocket = new Socket(ipRemote, PORT);
						System.out.println("Local client port: " + clientSocket.getLocalPort());
						out = new PrintWriter(clientSocket.getOutputStream(), true);
						in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
						onConnected(1);
						startListening();
					} catch (IOException e) {
						System.out.println("Connection failed!");
						e.printStackTrace();
						disconnect();
					}
				}
			}).start();
		} catch (UnknownHostException e) {
			System.out.println("Unknown host: " + address);
		}
	}

	void disconnect() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (clientSocket != null) {
					try {
						clientSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println("Disconnected from " + ipRemote.getHostAddress() + ":" + PORT);
					in = null;
					out = null;
					clientSocket = null;
					ipRemote = null;
				}
				if (serverSocket != null) { 
					try {
						serverSocket.close();
						serverSocket = null;
						System.out.println("Stopped host");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				onDisconnected();
			}
		}).start();
	}

	private void sendGameData() {
		if (out == null) {
			return;
		}
		String[] data = new String[3];
		data[MESSAGE_TYPE] = Integer.toString(MESSAGE_TYPE_GAME);
		data[IS_STARTED] = Boolean.toString(Simulation.isStarted());
		data[IS_PAUSED] = Boolean.toString(Simulation.isPaused());
		out.println(String.join(" ", data));
	}

	void sendLevel() {
		if (isClient || out == null) {
			return;
		}
		String[] data = new String[3];
		data[MESSAGE_TYPE] = Integer.toString(MESSAGE_TYPE_LEVEL);
		data[1] = Integer.toString(LevelManager.getLevel());
		out.println(String.join(" ", data));
	}

	/** Sends all data for all asteroids. */
	void sendAsteroids() {
		if (isClient || out == null) {
			return;
		}
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
		out.println(String.join(" ", data));
	}

	/**
	 * Sends data for the specified ship, including its bullets.
	 * @param ship The ship to send.
	 */
	private void sendShipData(Ship ship) {
		if (out == null) {
			return;
		}
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
		synchronized (ship.getBullets()) {
			for (int i = 0; i < ship.getBullets().size(); i++) {
				Bullet b = ship.getBullets().get(i);
				data[i*SHIP_BULLET_DATA_SIZE+SHIP_BULLET_POSX] = Float.toString(b.pos.x);
				data[i*SHIP_BULLET_DATA_SIZE+SHIP_BULLET_POSY] = Float.toString(b.pos.y);
				data[i*SHIP_BULLET_DATA_SIZE+SHIP_BULLET_DIR] = Integer.toString(b.direction);
			}
		}
		out.println(String.join(" ", data));
	}

	/**
	 * Client: Sends data for the local ship.
	 * Server: Sends data for all ships.
	 */
	void sendShipUpdate() {
		if (isClient && !ShipManager.getLocalShip().isDestroyed()) {
			sendShipData(ShipManager.getLocalShip());
		} else {
			List<Ship> ships = ShipManager.getShips();
			synchronized (ships) {
				for (Ship s : ships) {
					sendShipData(s);
				}
			}
		}
	}

	/**
	 * Sends the initial position and direction of a single bullet along with the ID of the player who shot it.
	 * @param bullet
	 */
	void sendFiredBullet(Bullet bullet) {
		if (out == null) {
			return;
		}
		String[] data = new String[5];
		data[MESSAGE_TYPE] = Integer.toString(MESSAGE_TYPE_BULLET);
		data[PLAYER_ID] = Integer.toString(bullet.getPlayerId());
		data[POSX] = Float.toString(bullet.pos.x);
		data[POSY] = Float.toString(bullet.pos.y);
		data[DIRECTION] = Integer.toString(bullet.direction);
		out.println(String.join(" ", data));
	}

	private String receive() {
		if (in == null) {
			return null;
		}
		String msg = null;
		try {
			msg = in.readLine();
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
	 * @param playerId ID of the local player.
	 */
	private void onConnected(int playerId) {
		System.out.println("Connected! Local player ID: " + playerId);
		isConnected = true;
		isClient = playerId != 0;
		connectionListener.onConnected();
		if (isClient) {
			try {
				ShipManager.addShip(new Ship(), 0);
			} catch (IOException e) {
				e.printStackTrace();
			}
			ShipManager.addShip(ShipManager.getLocalShip(), 1);
		} else {
			sendGameData();
			sendAsteroids();
			ShipManager.addShip(ShipManager.getLocalShip(), 0);
			try {
				ShipManager.addShip(new Ship(), 1);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void onDisconnected() {
		Simulation.setStarted(false);
		isConnected = false;
		isClient = false;
		connectionListener.onDisconnected();
		ShipManager.clearShips();
	}

	private void onReceiveGameData(boolean isStarted, boolean isPaused) {
		Simulation.setStarted(isStarted);
		Simulation.setPaused(isPaused);
	}

	private void onReceiveLevel(int level) {
		LevelManager.startLevel(level);
	}

	private void onReceivePlayerData(int playerId, float posX, float posY, int rotationDeg, boolean thrust,
			boolean isDestroyed, int score, int lives, List<Bullet> bullets) {
		List<Ship> ships = ShipManager.getShips();
		if (!isClient && playerId != 0) {
			Ship otherShip = ships.get(playerId);
			otherShip.forceUpdate(posX, posY, rotationDeg, thrust);
		} else if (isClient) {
			Ship ship = ships.get(playerId);
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

	private void onReceiveFiredBulletData(Bullet bullet) {
		if (!isClient) {
			List<Bullet> bullets = ShipManager.getShips().get(bullet.getPlayerId()).getBullets();
			synchronized (bullets) {
				bullets.add(bullet);
			}
		}
		Audio.SHOOT.play();
	}

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
}
