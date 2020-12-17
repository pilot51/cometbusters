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

import MultiplayerManager.ConnectionStateListener
import Simulation.GameStateListener
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.font.TextLayout
import java.io.IOException
import java.util.*
import java.util.Timer
import javax.imageio.ImageIO
import javax.swing.*

/**
 * Creates the game view and objects required within it.
 * @throws IOException if any images could not be read.
 */
class GameView @Throws(IOException::class) internal constructor() : JComponent(), KeyListener {
	private val imgBg: Image

	init {
		preferredSize = Dimension(VIEW_WIDTH, VIEW_HEIGHT)
		imgBg = ImageIO.read(javaClass.getResource("background.png"))
		Audio.init()
		Bullet.init()
		LevelManager.createBackgroundAsteroids()
		addKeyListener(this)
		isFocusable = true
		Timer().schedule(object : TimerTask() {
			override fun run() {
				if (!Simulation.isPaused()) {
					Simulation.simulate()
					repaint()
				}
			}
		}, (1000 / Simulation.TICK_RATE).toLong(), (1000 / Simulation.TICK_RATE).toLong())
	}

	override fun paintComponent(g: Graphics) {
		super.paintComponent(g)
		val g2d = g as Graphics2D
		g2d.drawImage(imgBg, 0, 0, null)
		g2d.font = Font("Arial", Font.PLAIN, 22)
		val ships = ShipManager.ships
		synchronized(ships) {
			for ((i, ship) in ships.withIndex()) {
				ship ?: continue
				val x = if (i == 0 || i == 3) 30 else VIEW_WIDTH - 118
				val y = if (i == 0 || i == 2) 30 else VIEW_HEIGHT - 42
				g2d.color = RenderUtils.PLAYER_COLORS[i]
				g2d.drawString(String.format("%07d", ship.score), x, y)
				RenderUtils.drawLives(g2d, ship.lives, RenderUtils.PLAYER_COLORS[i], x, y + 14)
			}
		}
		Asteroid.drawAsteroids(g2d)
		synchronized(ships) {
			for (s in ships) {
				if (s == null) continue
				s.drawShip(g2d)
				Bullet.drawBullets(g2d, s)
			}
		}
		if (LevelManager.isWaitingToStartLevel) {
			val font = Font(Font.SANS_SERIF, Font.BOLD, 100)
			g2d.font = font
			g2d.color = RenderUtils.TEXT_LEVEL_COLOR
			val text = "LEVEL ${LevelManager.level}"
			val textBounds = TextLayout(text, font, g2d.fontRenderContext).bounds
			g2d.drawString(text, VIEW_WIDTH / 2 - textBounds.width.toInt() / 2, VIEW_HEIGHT / 4 + textBounds.height.toInt() / 2)
		} else if (LevelManager.shouldShowText() && LevelManager.isGameOver) {
			val font = Font(Font.SANS_SERIF, Font.BOLD, 100)
			g2d.font = font
			g2d.color = RenderUtils.TEXT_GAMEOVER_COLOR
			val text = "GAME OVER"
			val textBounds = TextLayout(text, font, g2d.fontRenderContext).bounds
			g2d.drawString(text, VIEW_WIDTH / 2 - textBounds.width.toInt() / 2, VIEW_HEIGHT / 2 + textBounds.height.toInt() / 2)
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
		val buttonSound = JButton("Sound")
		val buttonMusic = JButton("Music")
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
			MultiplayerManager.instance.startHost()
		}
		menuConnect.addActionListener {
			val address = JOptionPane.showInputDialog(this@GameView, "Address", "Connect", JOptionPane.QUESTION_MESSAGE)
			if (address != null) {
				MultiplayerManager.instance.connect(address)
			}
		}
		menuDisconnect.addActionListener { MultiplayerManager.instance.disconnect() }
		buttonPause.addActionListener { Simulation.setPaused(!Simulation.isPaused()) }
		buttonSound.addActionListener { buttonSound.text = if (Audio.toggleSound()) "Sound" else "(sound)" }
		buttonMusic.addActionListener {
			buttonMusic.text = if (Audio.toggleMusic()) "Music" else "(music)"
			if (Simulation.isStarted) {
				Audio.MUSIC_GAME.loop()
			}
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
		Simulation.addGameStateListener(object : GameStateListener {
			override fun onGameStartStateChanged(started: Boolean) {
				buttonStart.text = if (started) "Stop" else "Start"
				if (MultiplayerManager.instance.isClient) {
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
				if (MultiplayerManager.instance.isClient) {
					Simulation.setPaused(paused)
				}
			}
		})
		MultiplayerManager.instance.setConnectionStateListener(object : ConnectionStateListener {
			override fun onHostWaiting() {
				menuHost.isVisible = false
				menuConnect.isVisible = false
				menuDisconnect.isVisible = true
			}

			override fun onConnected() {
				menuHost.isVisible = false
				menuConnect.isVisible = false
				menuDisconnect.isVisible = true
				if (MultiplayerManager.instance.isClient) {
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

	override fun keyPressed(e: KeyEvent) {
		val ship = ShipManager.localShip
		if (e.isConsumed || ship.isDestroyed || Simulation.isPaused()) return
		when (e.keyCode) {
			KeyEvent.VK_UP -> ship.thrust(true)
			KeyEvent.VK_LEFT -> ship.rotateLeft()
			KeyEvent.VK_RIGHT -> ship.rotateRight()
			KeyEvent.VK_SPACE, KeyEvent.VK_CONTROL -> ship.fire()
		}
	}

	override fun keyReleased(e: KeyEvent) {
		if (e.isConsumed) return
		val ship = ShipManager.localShip
		when (e.keyCode) {
			KeyEvent.VK_UP -> ship.thrust(false)
			KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT -> ship.rotateStop()
		}
	}

	override fun keyTyped(e: KeyEvent) {}

	companion object {
		private const val serialVersionUID = 1L
		const val VIEW_WIDTH = 1024
		const val VIEW_HEIGHT = 768
	}
}
