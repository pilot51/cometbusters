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
import java.awt.geom.AffineTransform;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

public class GameView extends JComponent implements KeyListener {
	private static final long serialVersionUID = 1L;
	
	private static final int VIEW_WIDTH = 1024, VIEW_HEIGHT = 768;
	private Image imgBg, imgShip;
	private int shipCenter, rotateDeg, rotateSpeed;
	
	GameView() {
		setPreferredSize(new Dimension(VIEW_WIDTH, VIEW_HEIGHT));
		try {
			imgBg = ImageIO.read(getClass().getClassLoader().getResource("img/background.png"));
			imgShip = ImageIO.read(getClass().getClassLoader().getResource("img/ship.png"));
			shipCenter = imgShip.getWidth(null) / 2;
		} catch (IOException e) {
			e.printStackTrace();
		}
		addKeyListener(this);
		setFocusable(true);
	}
	
	private AffineTransform trans = new AffineTransform();
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D)g;
		g2d.drawImage(imgBg, 0, 0, null);
		calculateMotion();
		trans.setToTranslation(VIEW_WIDTH / 2 - shipCenter, VIEW_HEIGHT / 2 - shipCenter);
		trans.rotate(Math.toRadians(rotateDeg), shipCenter, shipCenter);
		g2d.drawImage(imgShip, trans, null);
		repaint();
	}
	
	private void calculateMotion() {
		rotateDeg += rotateSpeed;
		if (rotateDeg == -1) {
			rotateDeg = 359;
		} else if (rotateDeg == 360) {
			rotateDeg = 0;
		}
	}
	
	@Override
	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
			case KeyEvent.VK_LEFT:
				rotateSpeed = -1;
				break;
			case KeyEvent.VK_RIGHT:
				rotateSpeed = 1;
				break;
		}
	}
	
	@Override
	public void keyReleased(KeyEvent e) {
		switch (e.getKeyCode()) {
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_RIGHT:
				rotateSpeed = 0;
				break;
		}
	}
	
	@Override
	public void keyTyped(KeyEvent e) {}
}
