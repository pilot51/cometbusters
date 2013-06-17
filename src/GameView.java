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
	
	GameView() throws IOException {
		setPreferredSize(new Dimension(VIEW_WIDTH, VIEW_HEIGHT));
		imgBg = ImageIO.read(getClass().getClassLoader().getResource("img/background.png"));
		ship = new Ship(ImageIO.read(getClass().getClassLoader().getResource("img/ship.png")),
		                VIEW_WIDTH / 2, VIEW_HEIGHT / 2);
		addKeyListener(this);
		setFocusable(true);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D)g;
		g2d.drawImage(imgBg, 0, 0, null);
		ship.calculateMotion();
		g2d.drawImage(ship.getImage(), ship.getTransform(), null);
		repaint();
	}
	
	@Override
	public void keyPressed(KeyEvent e) {
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
		}
	}
	
	@Override
	public void keyReleased(KeyEvent e) {
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
