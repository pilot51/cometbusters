/*
 * Copyright 2013 Mark Injerd
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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

public class GameView extends JComponent implements KeyListener {
	private static final long serialVersionUID = 1L;
	
	static final int VIEW_WIDTH = 1024, VIEW_HEIGHT = 768;
	private Image imgBg;
	
	/**
	 * Creates the game view and objects required within it.
	 * @throws IOException if any images could not be read.
	 */
	GameView() throws IOException {
		setPreferredSize(new Dimension(VIEW_WIDTH, VIEW_HEIGHT));
		imgBg = ImageIO.read(getClass().getClassLoader().getResource("img/background.png"));
		Audio.init();
		Bullet.init();
		LevelManager.createBackgroundAsteroids();
		addKeyListener(this);
		setFocusable(true);
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				if (!Simulation.isPaused()) {
					Simulation.simulate();
					repaint();
				}
			}
		}, 1000 / Simulation.TICK_RATE, 1000 / Simulation.TICK_RATE);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D)g;
		g2d.drawImage(imgBg, 0, 0, null);
		g2d.setFont(new Font("Arial", Font.PLAIN, 22));
		List<Ship> ships = ShipManager.getShips();
		for (int i = 0; i < ships.size(); i++) {
			int x = 0, y = 0;
			if (i == 0 || i == 3) {
				x = 30;
			} else {
				x = VIEW_WIDTH - 118;
			}
			if (i == 0 || i == 2) {
				y = 30;
			} else {
				y = VIEW_HEIGHT - 42;
			}
			g2d.setColor(RenderUtils.PLAYER_COLORS[i]);
			g2d.drawString(String.format("%07d", ships.get(i).getScore()), x, y);
			RenderUtils.drawLives(g2d, ships.get(i).getLives(), RenderUtils.PLAYER_COLORS[i], x, y + 14);
		}
		Asteroid.drawAsteroids(g2d);
		for (Ship s : ships) {
			s.drawShip(g2d);
			Bullet.drawBullets(g2d, s);
		}
		if (LevelManager.isWaitingToStartLevel()) {
			Font font = new Font(Font.SANS_SERIF, Font.BOLD, 100);
			g2d.setFont(font);
			g2d.setColor(RenderUtils.TEXT_LEVEL_COLOR);
			String text = "LEVEL " + LevelManager.getLevel();
			Rectangle2D textBounds = new TextLayout(text, font, g2d.getFontRenderContext()).getBounds();
			g2d.drawString(text, VIEW_WIDTH / 2 - (int)textBounds.getWidth() / 2, VIEW_HEIGHT / 4 + (int)textBounds.getHeight() / 2);
		} else if (LevelManager.shouldShowText() && LevelManager.isGameOver()) {
			Font font = new Font(Font.SANS_SERIF, Font.BOLD, 100);
			g2d.setFont(font);
			g2d.setColor(RenderUtils.TEXT_GAMEOVER_COLOR);
			String text = "GAME OVER";
			Rectangle2D textBounds = new TextLayout(text, font, g2d.getFontRenderContext()).getBounds();
			g2d.drawString(text, VIEW_WIDTH / 2 - (int)textBounds.getWidth() / 2, VIEW_HEIGHT / 2 + (int)textBounds.getHeight() / 2);
		}
		getToolkit().sync();
	}
	
	JMenuBar createMenu() {
		JMenuBar menuBar = new JMenuBar();
		JButton button1 = new JButton("1");
		JButton button2 = new JButton("2");
		JButton button3 = new JButton("3");
		JButton button4 = new JButton("4");
		JButton buttonStart = new JButton("Start");
		JButton buttonPlayers = new JButton("Players");
		JButton buttonGame = new JButton("Game");
		JPopupMenu popupGame = new JPopupMenu();
		final JMenuItem menuHost = new JMenuItem("Host", KeyEvent.VK_H);
		final JMenuItem menuConnect = new JMenuItem("Connect", KeyEvent.VK_N);
		final JMenuItem menuDisconnect = new JMenuItem("Disconnect", KeyEvent.VK_D);
		JButton buttonPause = new JButton("Pause");
		JButton buttonSound = new JButton("Sound");
		JButton buttonMusic = new JButton("Music");
		JButton buttonHelp = new JButton("Help");
		JButton buttonAbout = new JButton("About");
		menuDisconnect.setVisible(false);
		menuBar.setFocusable(false);
		button1.setFocusable(false);
		button2.setFocusable(false);
		button3.setFocusable(false);
		button4.setFocusable(false);
		buttonStart.setFocusable(false);
		buttonPlayers.setFocusable(false);
		buttonGame.setFocusable(false);
		buttonPause.setFocusable(false);
		buttonSound.setFocusable(false);
		buttonMusic.setFocusable(false);
		buttonHelp.setFocusable(false);
		buttonAbout.setFocusable(false);
		button1.setMnemonic(KeyEvent.VK_1);
		button2.setMnemonic(KeyEvent.VK_2);
		button3.setMnemonic(KeyEvent.VK_3);
		button4.setMnemonic(KeyEvent.VK_4);
		buttonStart.setMnemonic(KeyEvent.VK_T);
		buttonPlayers.setMnemonic(KeyEvent.VK_L);
		buttonGame.setMnemonic(KeyEvent.VK_G);
		buttonPause.setMnemonic(KeyEvent.VK_P);
		buttonSound.setMnemonic(KeyEvent.VK_S);
		buttonMusic.setMnemonic(KeyEvent.VK_M);
		buttonHelp.setMnemonic(KeyEvent.VK_H);
		buttonAbout.setMnemonic(KeyEvent.VK_A);
		buttonStart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (Simulation.isStarted()) {
					LevelManager.stopGame();
				} else {
					LevelManager.startGame();
				}
			}
		});
		buttonGame.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				popupGame.show(buttonGame, 0, buttonGame.getBounds().height);
			}
		});
		menuHost.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Simulation.setStarted(false);
				MultiplayerManager.getInstance().startHost();
			}
		});
		menuConnect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String address = (String)JOptionPane.showInputDialog(GameView.this, "Address", "Connect", JOptionPane.QUESTION_MESSAGE);
				if (address != null) {
					MultiplayerManager.getInstance().connect(address);
				}
			}
		});
		menuDisconnect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MultiplayerManager.getInstance().disconnect();
			}
		});
		buttonPause.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Simulation.setPaused(!Simulation.isPaused());
			}
		});
		buttonSound.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				buttonSound.setText(Audio.toggleSound() ? "Sound" : "(sound)");
			}
		});
		buttonMusic.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				buttonMusic.setText(Audio.toggleMusic() ? "Music" : "(music)");
				if (Simulation.isStarted()) {
					Audio.MUSIC_GAME.loop();
				}
			}
		});
		popupGame.add(menuHost);
		popupGame.add(menuConnect);
		popupGame.add(menuDisconnect);
		menuBar.add(button1);
		menuBar.add(button2);
		menuBar.add(button3);
		menuBar.add(button4);
		menuBar.add(Box.createHorizontalStrut(10));
		menuBar.add(buttonStart);
		menuBar.add(buttonPlayers);
		menuBar.add(buttonGame);
		menuBar.add(buttonPause);
		menuBar.add(buttonSound);
		menuBar.add(buttonMusic);
		menuBar.add(buttonHelp);
		menuBar.add(buttonAbout);
		Simulation.addGameStateListener(new Simulation.GameStateListener() {
			@Override
			public void onGameStartStateChanged(boolean started) {
				buttonStart.setText(started ? "Stop" : "Start");
				if (MultiplayerManager.getInstance().isClient()) {
					buttonStart.setEnabled(false);
					if (started) {
						LevelManager.startGame();
					} else if (!LevelManager.isGameOver()) {
						LevelManager.stopGame();
					}
				}
			}

			@Override
			public void onGamePauseStateChanged(boolean paused) {
				buttonPause.setText(paused ? "Continue" : "Pause");
				buttonPause.setMnemonic(paused ? KeyEvent.VK_C : KeyEvent.VK_P);
				if (MultiplayerManager.getInstance().isClient()) {
					Simulation.setPaused(paused);
				}
			}
		});
		MultiplayerManager.getInstance().setConnectionStateListener(new MultiplayerManager.ConnectionStateListener() {
			@Override
			public void onHostWaiting() {
				menuHost.setVisible(false);
				menuConnect.setVisible(false);
				menuDisconnect.setVisible(true);
			}
			@Override
			public void onConnected() {
				menuHost.setVisible(false);
				menuConnect.setVisible(false);
				menuDisconnect.setVisible(true);
				if (MultiplayerManager.getInstance().isClient()) {
					buttonStart.setEnabled(false);
					buttonPause.setEnabled(false);
				}
			}

			@Override
			public void onDisconnected() {
				menuHost.setVisible(true);
				menuConnect.setVisible(true);
				menuDisconnect.setVisible(false);
				buttonStart.setEnabled(true);
				buttonPause.setEnabled(true);
			}
		});
		return menuBar;
	}

	@Override
	public void keyPressed(KeyEvent e) {
		Ship ship = ShipManager.getLocalShip();
		if (e.isConsumed() || ship.isDestroyed() || Simulation.isPaused()) return;
		switch (e.getKeyCode()) {
			case KeyEvent.VK_UP:
				ship.thrust(true);
				break;
			case KeyEvent.VK_LEFT:
				ship.rotateLeft();
				break;
			case KeyEvent.VK_RIGHT:
				ship.rotateRight();
				break;
			case KeyEvent.VK_CONTROL:
				ship.fire();
				break;
		}
	}
	
	@Override
	public void keyReleased(KeyEvent e) {
		if (e.isConsumed()) return;
		Ship ship = ShipManager.getLocalShip();
		switch (e.getKeyCode()) {
			case KeyEvent.VK_UP:
				ship.thrust(false);
				break;
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_RIGHT:
				ship.rotateStop();
				break;
		}
	}
	
	@Override
	public void keyTyped(KeyEvent e) {}
}
