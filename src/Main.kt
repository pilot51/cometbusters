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

import java.io.IOException
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

class Main private constructor() : JFrame("Comet Busters") {
	companion object {
		private const val serialVersionUID = 1L
		@JvmStatic
		fun main(args: Array<String>) {
			SwingUtilities.invokeLater { Main() }
		}
	}

	init {
		defaultCloseOperation = EXIT_ON_CLOSE
		iconImage = ImageIcon(javaClass.getResource("icon.png")).image
		try {
			val gv = GameView()
			jMenuBar = gv.createMenu()
			add(gv)
			pack()
			isVisible = true
		} catch (e: IOException) {
			System.err.println("Error loading images!")
			e.printStackTrace()
			exitProcess(1)
		}
	}
}
