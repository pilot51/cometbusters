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
	private static final int MESSAGE_TYPE_GAME = 0, MESSAGE_TYPE_PLAYER = 1, MESSAGE_TYPE_ASTEROIDS = 2, MESSAGE_TYPE_BULLET = 3;
	private static final int MESSAGE_TYPE = 0, IS_STARTED = 1, IS_PAUSED = 2,
			PLAYER_ID = 1, POSX = 2, POSY = 3, ROTATION = 4, ACCEL = 5, DESTROYED = 6, SCORE = 7;
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
						case MESSAGE_TYPE_ASTEROIDS:
							List<Asteroid> asteroids = null;
							if (data.length > 1) {
								asteroids = new ArrayList<Asteroid>();
								for (int i = 1; i + 4 < data.length; i += 5) {
									asteroids.add(new Asteroid(Float.parseFloat(data[i]), Float.parseFloat(data[i+1]), Integer.parseInt(data[i+2]),
											Integer.parseInt(data[i+3]), Asteroid.Size.valueOf(data[i+4])));
								}
							}
							onReceiveAsteroidData(asteroids);
							break;
						case MESSAGE_TYPE_PLAYER:
							List<Bullet> bullets = null;
							if (data.length > 7) {
								bullets = new ArrayList<Bullet>();
								for (int i = 8; i + 2 < data.length; i += 3) {
									bullets.add(new Bullet(Integer.parseInt(data[PLAYER_ID]), Float.parseFloat(data[i]),
											Float.parseFloat(data[i+1]), Integer.parseInt(data[i+2])));
								}
							}
							onReceivePlayerData(Integer.parseInt(data[PLAYER_ID]), Float.parseFloat(data[POSX]),
									Float.parseFloat(data[POSY]), Integer.parseInt(data[ROTATION]), Boolean.parseBoolean(data[ACCEL]),
									Boolean.parseBoolean(data[DESTROYED]), Integer.parseInt(data[SCORE]), bullets);
							break;
						case MESSAGE_TYPE_BULLET:
							onReceiveFiredBulletData(new Bullet(Integer.parseInt(data[PLAYER_ID]), Float.parseFloat(data[POSX]),
									Float.parseFloat(data[POSY]), Integer.parseInt(data[ROTATION])));
							break;
						}
					}
				};
			}
		}).start();
	}

	void startHost() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					serverSocket = new ServerSocket(PORT);
				} catch (IOException e) {
					System.out.println("Could not listen on port " + PORT);
				}
				connectionListener.onHostWaiting();
				try{
					clientSocket = serverSocket.accept();
					ipRemote = clientSocket.getInetAddress();
					System.out.println("Incoming connection: " + ipRemote.getHostAddress() + ":" + PORT);
					in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
					out = new PrintWriter(clientSocket.getOutputStream(), true);
					onConnected(0);
					startListening();
				} catch (IOException e) {
					System.out.println("Connection failed on port " + PORT);
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
						out = new PrintWriter(clientSocket.getOutputStream(), true);
						in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
						onConnected(1);
						startListening();
					} catch (UnknownHostException e) {
						System.out.println("Unknown host: " + ipRemote.getHostAddress());
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();
		} catch (UnknownHostException e) {
			e.printStackTrace();
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
					System.out.println("Disconnected: " + ipRemote.getHostAddress() + ":" + PORT);
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
		String data = MESSAGE_TYPE_GAME + " " + Simulation.isStarted() + " " + Simulation.isPaused();
		out.println(data);
	}

	/** Sends all data for all asteroids. */
	void sendAsteroids() {
		if (isClient || out == null) {
			return;
		}
		String data = Integer.toString(MESSAGE_TYPE_ASTEROIDS);
		synchronized (Asteroid.getAsteroids()) {
			for (Asteroid a : Asteroid.getAsteroids()) {
				data += " " + a.pos.x + " " + a.pos.y + " " + a.direction + " " + a.velocity + " " + a.getSize().name();
			}
		}
		out.println(data);
	}

	/**
	 * Sends data for the specified ship, including its bullets.
	 * @param ship The ship to send.
	 */
	private void sendShipData(Ship ship) {
		if (out == null) {
			return;
		}
		String data = MESSAGE_TYPE_PLAYER + " " + ShipManager.getShips().indexOf(ship) + " " + ship.pos.x + " " + ship.pos.y
				+ " " + ship.rotateDeg + " " + ship.isAccelerating + " " + ship.isDestroyed() + " " + ship.getScore();
		synchronized (ship.getBullets()) {
			for (Bullet b : ship.getBullets()) {
				data += " " + b.pos.x + " " + b.pos.y + " " + b.direction;
			}
		}
		out.println(data);
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
		String data = MESSAGE_TYPE_BULLET + " " + bullet.getPlayerId() + " " + bullet.pos.x + " " + bullet.pos.y + " " + bullet.direction;
		out.println(data);
	}

	private String receive() {
		if (in == null) {
			return null;
		}
		String msg;
		try {
			msg = in.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			disconnect();
			return null;
		}
		if (msg != null) {
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
		System.out.println("onConnected: " + playerId);
		isConnected = true;
		List<Ship> ships = ShipManager.getShips();
		isClient = playerId != 0;
		connectionListener.onConnected();
		synchronized (ships) {
			if (isClient) {
				try {
					Ship hostShip = new Ship();
					ships.set(0, hostShip);
				} catch (IOException e) {
					e.printStackTrace();
				}
				Ship ship = ShipManager.getLocalShip();
				ships.add(ship);
			} else {
				sendGameData();
				sendAsteroids();
				Ship ship = ShipManager.getLocalShip();
				ships.set(0, ship);
				try {
					Ship clientShip = new Ship();
					ships.add(1, clientShip);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void onDisconnected() {
		System.out.println("onDisconnected");
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

	private void onReceivePlayerData(int playerId, float posX, float posY, int rotationDeg, boolean thrust, boolean isDestroyed, int score, List<Bullet> bullets) {
		List<Ship> ships = ShipManager.getShips();
		if (!isClient && playerId != 0) {
			Ship otherShip = ships.get(playerId);
			otherShip.forceUpdate(posX, posY, rotationDeg, thrust);
		} else if (isClient) {
			Ship ship = ships.get(playerId);
			ship.addScore(score - ship.getScore());
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
			ShipManager.getShips().get(bullet.getPlayerId()).getBullets().add(bullet);
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
