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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuBar;

public class GameView extends JComponent implements KeyListener {
	private static final long serialVersionUID = 1L;
	
	static final int VIEW_WIDTH = 1024, VIEW_HEIGHT = 768;
	private Ship ship;
	private Image imgBg;
	private boolean[] keysPressed = new boolean[1024];
	
	/**
	 * Creates the game view and objects required within it.
	 * @throws IOException if any images could not be read.
	 */
	GameView() throws IOException {
		setPreferredSize(new Dimension(VIEW_WIDTH, VIEW_HEIGHT));
		imgBg = ImageIO.read(getClass().getClassLoader().getResource("img/background.png"));
		ship = new Ship();
		Sound.init();
		Bullet.init();
		Asteroid.generateAsteroids(1);
		addKeyListener(this);
		setFocusable(true);
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				if (!Simulation.isPaused) {
					Simulation.simulate(ship);
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
		g2d.setColor(Color.decode("#00FFFF"));
		g2d.setFont(new Font("Arial", Font.PLAIN, 18));
		g2d.drawString(String.format("%07d", ship.getScore()), 30, 30);
		Asteroid.drawAsteroids(g2d);
		ship.drawShip(g2d);
		Bullet.drawBullets(g2d, ship);
		getToolkit().sync();
	}
	
	JMenuBar createMenu() {
		JMenuBar menuBar = new JMenuBar();
		menuBar.setFocusable(false);
		JButton button1 = new JButton("1");
		button1.setMnemonic(KeyEvent.VK_1);
		button1.setFocusable(false);
		menuBar.add(button1);
		JButton button2 = new JButton("2");
		button2.setMnemonic(KeyEvent.VK_2);
		button2.setFocusable(false);
		menuBar.add(button2);
		JButton button3 = new JButton("3");
		button3.setMnemonic(KeyEvent.VK_3);
		button3.setFocusable(false);
		menuBar.add(button3);
		JButton button4 = new JButton("4");
		button4.setMnemonic(KeyEvent.VK_4);
		button4.setFocusable(false);
		menuBar.add(button4);
		menuBar.add(Box.createHorizontalStrut(10));
		JButton buttonStart = new JButton("Start");
		buttonStart.setMnemonic(KeyEvent.VK_T);
		buttonStart.setFocusable(false);
		buttonStart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Simulation.isStarted ^= true;
				if (Simulation.isStarted) {
					startGame();
				} else {
					stopGame();
				}
				buttonStart.setText(Simulation.isStarted ? "Stop" : "Start");
			}
		});
		menuBar.add(buttonStart);
		JButton buttonPlayers = new JButton("Players");
		buttonPlayers.setMnemonic(KeyEvent.VK_L);
		buttonPlayers.setFocusable(false);
		menuBar.add(buttonPlayers);
		JButton buttonGame = new JButton("Game");
		buttonGame.setMnemonic(KeyEvent.VK_G);
		buttonGame.setFocusable(false);
		menuBar.add(buttonGame);
		JButton buttonPause = new JButton("Pause");
		buttonPause.setMnemonic(KeyEvent.VK_P);
		buttonPause.setFocusable(false);
		buttonPause.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Simulation.isPaused ^= true;
				buttonPause.setMnemonic(Simulation.isPaused ? KeyEvent.VK_C : KeyEvent.VK_P);
				buttonPause.setText(Simulation.isPaused ? "Continue" : "Pause");
			}
		});
		menuBar.add(buttonPause);
		JButton buttonSound = new JButton("Sound");
		buttonSound.setMnemonic(KeyEvent.VK_S);
		buttonSound.setFocusable(false);
		buttonSound.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				buttonSound.setText(Sound.toggleSound() ? "Sound" : "(sound)");
			}
		});
		menuBar.add(buttonSound);
		JButton buttonMusic = new JButton("Music");
		buttonMusic.setMnemonic(KeyEvent.VK_M);
		buttonMusic.setFocusable(false);
		menuBar.add(buttonMusic);
		JButton buttonHelp = new JButton("Help");
		buttonHelp.setMnemonic(KeyEvent.VK_H);
		buttonHelp.setFocusable(false);
		menuBar.add(buttonHelp);
		JButton buttonAbout = new JButton("About");
		buttonAbout.setMnemonic(KeyEvent.VK_A);
		buttonAbout.setFocusable(false);
		menuBar.add(buttonAbout);
		return menuBar;
	}

	private void startGame() {
		try {
			Asteroid.generateAsteroids(1);
			ship.spawn(VIEW_WIDTH / 2, VIEW_HEIGHT / 2);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void stopGame() {
		ship.terminate();
		try {
			Asteroid.generateAsteroids(1);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void keyPressed(KeyEvent e) {
		if (e.isConsumed() || ship.isDestroyed() || Simulation.isPaused) return;
		keysPressed[e.getKeyCode()] = true;
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
		KeyEvent nextPress = (KeyEvent)Toolkit.getDefaultToolkit().getSystemEventQueue().peekEvent(KeyEvent.KEY_PRESSED);
		if (nextPress != null && nextPress.getWhen() == e.getWhen()) return;
		keysPressed[e.getKeyCode()] = false;
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
