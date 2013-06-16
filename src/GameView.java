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
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

public class GameView extends JComponent {
	private static final long serialVersionUID = 1L;
	
	private static final int VIEW_WIDTH = 1024, VIEW_HEIGHT = 768;
	
	GameView() {
		setPreferredSize(new Dimension(VIEW_WIDTH, VIEW_HEIGHT));
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		try {
			g.drawImage(ImageIO.read(getClass().getClassLoader().getResource("img/background.png")),
			            0, 0, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
