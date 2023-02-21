/*
 * Copyright 2013-2023 Mark Injerd
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

import Platform.Renderer.RenderView2D
import Platform.Renderer.TextLayout
import Platform.Resources.Font
import Platform.Resources.Image
import Platform.Utils.Timer
import Platform.Utils.toZeroPaddedString
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.io.IOException
import java.util.*
import javax.swing.*
import MultiplayerManager.Companion.instance as mpMan

/**
 * Creates the game view and objects required within it.
 * @throws IOException if any images could not be read.
 */
actual class GameView @Throws(IOException::class) constructor() : JComponent(), KeyListener {
	private val imgBg = Image("img/background.png")

	init {
		preferredSize = Dimension(VIEW_WIDTH, VIEW_HEIGHT)
		LevelManager.createBackgroundAsteroids()
		addKeyListener(this)
		isFocusable = true
		Timer().run((1000 / Simulation.TICK_RATE).toLong(), (1000 / Simulation.TICK_RATE).toLong()) {
			if (!Simulation.isPaused()) {
				Simulation.simulate()
				repaint()
			}
		}
	}

	override fun paintComponent(g: Graphics) {
		super.paintComponent(g)
		val view2D = RenderView2D(g)
		view2D.drawImage(imgBg, 0, 0)
		view2D.setFont(Font(Font.ARIAL, Font.Style.PLAIN, 22))
		val ships = ShipManager.ships
		ships.filterNotNull().forEachIndexed { i, ship ->
			val x = if (i == 0 || i == 3) 30 else VIEW_WIDTH - 118
			val y = if (i == 0 || i == 2) 30 else VIEW_HEIGHT - 42
			val color = RenderUtils.PLAYER_COLORS[i]
			view2D.setColor(color)
			view2D.drawText(ship.score.toZeroPaddedString(7), x, y)
			RenderUtils.drawLives(view2D, ship, color, x, y + 14)
		}
		Asteroid.drawAsteroids(view2D)
		ships.filterNotNull().forEach {
			it.drawShip(view2D)
			Bullet.drawBullets(view2D, it)
		}
		if (LevelManager.isWaitingToStartLevel) {
			val font = Font(Font.SANS_SERIF, Font.Style.BOLD, 100)
			view2D.setFont(font)
			view2D.setColor(RenderUtils.TEXT_LEVEL_COLOR)
			val text = "LEVEL ${LevelManager.level}"
			val textBounds = TextLayout(text, font, view2D.fontRenderContext).bounds
			view2D.drawText(
				text,
				VIEW_WIDTH / 2 - textBounds.width.toInt() / 2,
				VIEW_HEIGHT / 4 + textBounds.height.toInt() / 2
			)
		} else if (LevelManager.shouldShowText && LevelManager.isGameOver) {
			val font = Font(Font.SANS_SERIF, Font.Style.BOLD, 100)
			view2D.setFont(font)
			view2D.setColor(RenderUtils.TEXT_GAMEOVER_COLOR)
			val text = "GAME OVER"
			val textBounds = TextLayout(text, font, view2D.fontRenderContext).bounds
			view2D.drawText(
				text,
				VIEW_WIDTH / 2 - textBounds.width.toInt() / 2,
				VIEW_HEIGHT / 2 + textBounds.height.toInt() / 2
			)
		}
		toolkit.sync()
	}

	fun createMenu(): JMenuBar {
		val menuBar = JMenuBar()
		val button1 = JButton("1")
		val button2 = JButton("2")
		val button3 = JButton("3")
		val button4 = JButton("4")
		val buttonStart = JButton("Start")
		val buttonPlayers = JButton("Players")
		val buttonGame = JButton("Game")
		val popupGame = JPopupMenu()
		val menuHost = JMenuItem("Host", KeyEvent.VK_H)
		val menuConnect = JMenuItem("Connect", KeyEvent.VK_N)
		val menuDisconnect = JMenuItem("Disconnect", KeyEvent.VK_D)
		val buttonPause = JButton("Pause")
		val buttonSound = JButton(getSoundText())
		val buttonMusic = JButton(getMusicText())
		val buttonHelp = JButton("Help")
		val buttonAbout = JButton("About")
		menuDisconnect.isVisible = false
		menuBar.isFocusable = false
		button1.isFocusable = false
		button2.isFocusable = false
		button3.isFocusable = false
		button4.isFocusable = false
		buttonStart.isFocusable = false
		buttonPlayers.isFocusable = false
		buttonGame.isFocusable = false
		buttonPause.isFocusable = false
		buttonSound.isFocusable = false
		buttonMusic.isFocusable = false
		buttonHelp.isFocusable = false
		buttonAbout.isFocusable = false
		button1.mnemonic = KeyEvent.VK_1
		button2.mnemonic = KeyEvent.VK_2
		button3.mnemonic = KeyEvent.VK_3
		button4.mnemonic = KeyEvent.VK_4
		buttonStart.mnemonic = KeyEvent.VK_T
		buttonPlayers.mnemonic = KeyEvent.VK_L
		buttonGame.mnemonic = KeyEvent.VK_G
		buttonPause.mnemonic = KeyEvent.VK_P
		buttonSound.mnemonic = KeyEvent.VK_S
		buttonMusic.mnemonic = KeyEvent.VK_M
		buttonHelp.mnemonic = KeyEvent.VK_H
		buttonAbout.mnemonic = KeyEvent.VK_A
		button1.isEnabled = false
		button2.isEnabled = false
		button3.isEnabled = false
		button4.isEnabled = false
		buttonPlayers.isEnabled = false
		buttonHelp.isEnabled = false
		buttonAbout.isEnabled = false
		buttonStart.addActionListener {
			if (Simulation.isStarted) {
				LevelManager.stopGame()
			} else {
				LevelManager.startGame()
			}
		}
		buttonGame.addActionListener { popupGame.show(buttonGame, 0, buttonGame.bounds.height) }
		menuHost.addActionListener {
			Simulation.isStarted = false
			mpMan.startHost()
		}
		menuConnect.addActionListener {
			val address = JOptionPane.showInputDialog(this@GameView, "Address", "Connect", JOptionPane.QUESTION_MESSAGE)
			if (address != null) {
				mpMan.connect(address)
			}
		}
		menuDisconnect.addActionListener { mpMan.disconnect() }
		buttonPause.addActionListener { Simulation.setPaused(!Simulation.isPaused()) }
		buttonSound.addActionListener {
			buttonSound.text = getSoundText(Audio.toggleSound())
		}
		buttonMusic.addActionListener {
			buttonMusic.text = getMusicText(Audio.toggleMusic())
		}
		popupGame.add(menuHost)
		popupGame.add(menuConnect)
		popupGame.add(menuDisconnect)
		menuBar.add(button1)
		menuBar.add(button2)
		menuBar.add(button3)
		menuBar.add(button4)
		menuBar.add(Box.createHorizontalStrut(10))
		menuBar.add(buttonStart)
		menuBar.add(buttonPlayers)
		menuBar.add(buttonGame)
		menuBar.add(buttonPause)
		menuBar.add(buttonSound)
		menuBar.add(buttonMusic)
		menuBar.add(buttonHelp)
		menuBar.add(buttonAbout)
		Simulation.addGameStateListener(object : Simulation.GameStateListener {
			override fun onGameStartStateChanged(started: Boolean) {
				buttonStart.text = if (started) "Stop" else "Start"
				if (mpMan.isClient) {
					buttonStart.isEnabled = false
					if (started) {
						LevelManager.startGame()
					} else if (!LevelManager.isGameOver) {
						LevelManager.stopGame()
					}
				}
			}

			override fun onGamePauseStateChanged(paused: Boolean) {
				buttonPause.text = if (paused) "Continue" else "Pause"
				buttonPause.mnemonic = if (paused) KeyEvent.VK_C else KeyEvent.VK_P
				if (mpMan.isClient) {
					Simulation.setPaused(paused)
				}
			}
		})
		mpMan.setConnectionStateListener(object : MultiplayerManager.ConnectionStateListener {
			override fun onHostWaiting() {
				menuHost.isVisible = false
				menuConnect.isVisible = false
				menuDisconnect.isVisible = true
			}

			override fun onConnected() {
				menuHost.isVisible = false
				menuConnect.isVisible = false
				menuDisconnect.isVisible = true
				if (mpMan.isClient) {
					buttonStart.isEnabled = false
					buttonPause.isEnabled = false
				}
			}

			override fun onDisconnected() {
				menuHost.isVisible = true
				menuConnect.isVisible = true
				menuDisconnect.isVisible = false
				buttonStart.isEnabled = true
				buttonPause.isEnabled = true
			}
		})
		return menuBar
	}

	private fun getSoundText(soundEnabled: Boolean = Audio.isSoundEnabled) =
		if (soundEnabled) "Sound" else "(sound)"
	private fun getMusicText(musicEnabled: Boolean = Audio.isMusicEnabled) =
		if (musicEnabled) "Music" else "(music)"

	override fun keyPressed(e: KeyEvent) {
		val ship = ShipManager.localShip
		if (e.isConsumed || ship.isDestroyed || Simulation.isPaused()) return
		when (e.keyCode) {
			KeyEvent.VK_UP -> ship.thrust(activate = true, isFromInput = true)
			KeyEvent.VK_LEFT -> ship.rotateLeft()
			KeyEvent.VK_RIGHT -> ship.rotateRight()
			KeyEvent.VK_SPACE, KeyEvent.VK_CONTROL -> ship.fire()
		}
	}

	override fun keyReleased(e: KeyEvent) {
		if (e.isConsumed) return
		val ship = ShipManager.localShip
		when (e.keyCode) {
			KeyEvent.VK_UP -> ship.thrust(activate = false, isFromInput = true)
			KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT -> ship.rotateStop()
		}
	}

	override fun keyTyped(e: KeyEvent) {}

	actual companion object {
		private const val serialVersionUID = 1L
		actual val VIEW_WIDTH = 1024
		actual val VIEW_HEIGHT = 768
	}
}
