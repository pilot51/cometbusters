/*
 * Copyright 2020 Mark Injerd
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

import Platform.Renderer.Rectangle2D
import Platform.Renderer.RenderView2D
import Platform.Resources.Font
import Platform.Resources.Image
import Platform.Utils.Timer
import Platform.Utils.toZeroPaddedString
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.events.KeyboardEvent

/** Creates the game view and objects required within it. */
actual class GameView {
	private val renderView = RenderView2D(VIEW_WIDTH, VIEW_HEIGHT)
	private val btn1 = getButton("btn1")
	private val btn2 = getButton("btn2")
	private val btn3 = getButton("btn3")
	private val btn4 = getButton("btn4")
	private val btnStart = getButton("btnStart")
	private val btnPlayers = getButton("btnPlayers")
	private val btnGame = getButton("btnGame")
	private val btnPause = getButton("btnPause")
	private val btnSound = getButton("btnSound")
	private val btnMusic = getButton("btnMusic")
	private val btnHelp = getButton("btnHelp")
	private val btnAbout = getButton("btnAbout")
	private val pressedKeys = HashMap<String, Boolean>(5)

	init {
		document.body!!.append(renderView.jsCanvas)
		Audio.init()
		Bullet.init()
		LevelManager.createBackgroundAsteroids()
		initKeyListeners()
		window.onload = {
			Timer().run((1000 / Simulation.TICK_RATE).toLong(), (1000 / Simulation.TICK_RATE).toLong()) {
				if (!Simulation.isPaused()) {
					Simulation.simulate()
					update()
				}
			}
		}

		Simulation.addGameStateListener(object : Simulation.GameStateListener {
			override fun onGameStartStateChanged(started: Boolean) {
				btnStart.innerHTML = if (started) "S<u>t</u>op" else "S<u>t</u>art"
				if (MultiplayerManager.instance.isClient) {
					btnStart.disabled = true
					if (started) {
						LevelManager.startGame()
					} else if (!LevelManager.isGameOver) {
						LevelManager.stopGame()
					}
				}
			}

			override fun onGamePauseStateChanged(paused: Boolean) {
				btnPause.innerHTML = if (paused) "<u>C</u>ontinue" else "<u>P</u>ause"
				btnPause.accessKey = if (paused) "C" else "P"
				if (MultiplayerManager.instance.isClient) {
					Simulation.setPaused(paused)
				}
			}
		})
	}

	private fun update() {
		renderView.drawImage(Image("img/background.png"), 0, 0)
		renderView.setFont(Font(Font.ARIAL, Font.Style.PLAIN, 22))
		val ships = ShipManager.ships
		ships.filterNotNull().forEachIndexed { i, ship ->
			val x = if (i == 0 || i == 3) 30 else VIEW_WIDTH - 118
			val y = if (i == 0 || i == 2) 30 else VIEW_HEIGHT - 42
			val color = RenderUtils.PLAYER_COLORS[i]
			renderView.setColor(color)
			renderView.drawText(ship.score.toZeroPaddedString(7), x, y)
			RenderUtils.drawLives(renderView, ship.lives, color, x, y + 14)
		}
		Asteroid.drawAsteroids(renderView)
		ships.filterNotNull().forEach {
			it.drawShip(renderView)
			Bullet.drawBullets(renderView, it)
		}
		if (LevelManager.isWaitingToStartLevel) {
			val font = Font(Font.SANS_SERIF, Font.Style.BOLD, 100)
			renderView.setFont(font)
			renderView.setColor(RenderUtils.TEXT_LEVEL_COLOR)
			val text = "LEVEL ${LevelManager.level}"
			val textBounds = Rectangle2D.getTextRect(text, font)
			renderView.drawText(
				text,
				VIEW_WIDTH / 2 - textBounds.width.toInt() / 2,
				VIEW_HEIGHT / 4 + textBounds.height.toInt() / 2
			)
		} else if (LevelManager.shouldShowText && LevelManager.isGameOver) {
			val font = Font(Font.SANS_SERIF, Font.Style.BOLD, 100)
			renderView.setFont(font)
			renderView.setColor(RenderUtils.TEXT_GAMEOVER_COLOR)
			val text = "GAME OVER"
			val textBounds = Rectangle2D.getTextRect(text, font)
			renderView.drawText(
				text,
				VIEW_WIDTH / 2 - textBounds.width.toInt() / 2,
				VIEW_HEIGHT / 2 + textBounds.height.toInt() / 2
			)
		}
	}

	private fun getButton(id: String) = document.getElementById(id) as HTMLButtonElement

	private fun initKeyListeners() {
		document.addEventListener("keydown", {
			keyPressHandler("keydown", it as KeyboardEvent)
		})
		document.addEventListener("keyup", {
			keyPressHandler("keyup", it as KeyboardEvent)
		})
		btnStart.addEventListener("click", {
			if (Simulation.isStarted) {
				LevelManager.stopGame()
			} else {
				LevelManager.startGame()
			}
		})
		btnPause.addEventListener("click", {
			Simulation.setPaused(!Simulation.isPaused())
		})
		btnSound.addEventListener("click", {
			btnSound.innerHTML = if (Audio.toggleSound()) "<u>S</u>ound" else "(<u>s</u>ound)"
		})
		btnMusic.addEventListener("click", {
			btnMusic.innerHTML = if (Audio.toggleMusic()) "<u>M</u>usic" else "(<u>m</u>usic)"
		})
	}

	private fun <T> T.isAny(vararg objects: T) = objects.contains(this)

	private fun keyPressHandler(type: String, event: KeyboardEvent) {
		if (event.key.isAny(Key.UP, Key.LEFT, Key.RIGHT, Key.SPACE, Key.CTRL)) {
			event.preventDefault()
		} else return
		val ship = ShipManager.localShip
		if (event.repeat || ship.isDestroyed || Simulation.isPaused()) return
		when (type) {
			"keydown" -> {
				if (pressedKeys[event.key] == true) return
				pressedKeys[event.key] = true
				when (event.key) {
					Key.UP -> ship.thrust(true)
					Key.LEFT -> ship.rotateLeft()
					Key.RIGHT -> ship.rotateRight()
					Key.SPACE, Key.CTRL -> ship.fire()
				}
			}
			"keyup" -> {
				if (pressedKeys[event.key] != true) return
				when (event.code) {
					Key.UP -> ship.thrust(false)
					Key.LEFT, Key.RIGHT -> ship.rotateStop()
				}
				pressedKeys[event.key] = false
			}
		}
	}

	private object Key {
		const val UP = "ArrowUp"
		const val LEFT = "ArrowLeft"
		const val RIGHT = "ArrowRight"
		const val SPACE = " "
		const val CTRL = "Control"
	}

	actual companion object {
		actual val VIEW_WIDTH = 1024
		actual val VIEW_HEIGHT = 768
	}
}
