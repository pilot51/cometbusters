/*
 * Copyright 2015 Mark Injerd
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

import javax.swing.JApplet;

public class Applet extends JApplet {
	private static final long serialVersionUID = 1L;

	public void init() {
		try {
			GameView gv = new GameView();
			setJMenuBar(gv.createMenu());
			add(gv);
			setVisible(true);
			setSize(1024, 768);
		} catch (IOException e) {
			System.err.println("Error loading images!");
			e.printStackTrace();
			System.exit(1);
		}
	}
}
