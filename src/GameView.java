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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

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
		ship = new Ship(VIEW_WIDTH / 2, VIEW_HEIGHT / 2);
		Sound.init();
		Bullet.init();
		Asteroid.generateAsteroids(1);
		addKeyListener(this);
		setFocusable(true);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		for (Asteroid a : Asteroid.getAsteroids()) {
			if (ship.isContacting(a)) {
				ship.collide(a);
				break;
			}
		}
		Graphics2D g2d = (Graphics2D)g;
		g2d.drawImage(imgBg, 0, 0, null);
		Asteroid.drawAsteroids(g2d);
		ship.drawShip(g2d);
		Bullet.drawBullets(g2d, ship);
		repaint();
	}
	
	@Override
	public void keyPressed(KeyEvent e) {
		if (e.isConsumed() || ship.isDestroyed()) return;
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
