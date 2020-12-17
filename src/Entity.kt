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

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Creates an object at the given coordinates with initial rotation, initial velocity,
 * potential acceleration, and potential rotation speed.
 * @param x Initial x-position.
 * @param y Initial y-position.
 * @param direction Initial direction of travel in degrees.
 * @param velocity Initial forward velocity.
 * @param acceleration Forward acceleration applied when [isAccelerating] is true.
 * @param rotationSpeed Rotation speed applied when [rotateLeft] or [rotateRight] is called, stopped with [rotateStop].
 */
abstract class Entity @JvmOverloads internal constructor(
	x: Float,
	y: Float,
	/** Initial direction of travel in degrees. */
	val direction: Int,
	/** Initial velocity. */
	val velocity: Int,
	/** Forward acceleration applied when [isAccelerating] is true.  */
	private val acceleration: Int = 0,
	/** Rotation speed applied when [rotateLeft] or [rotateRight] is called, stopped with [rotateStop]. */
	private val rotationSpeed: Int = 0
) {
	val pos: Position = Position(x, y)
	private val radians = Math.toRadians(direction.toDouble())
	protected var velX: Float = sin(radians).toFloat() * velocity
	protected var velY: Float = cos(radians).toFloat() * velocity
	var radius = 0
		protected set
	/** True to accelerate at the predefined rate, false to stop accelerating. */
	var isAccelerating = false
		protected set

	/** Current rotation in degrees. */
	var rotateDeg = 0
		protected set

	/** Current rotation direction and speed. Positive is clockwise, negative is counter-clockwise. */
	private var rotation = 0

	/**
	 * True if this entity is destroyed (not active), false if not.
	 * @see destroy
	 * @see undestroy
	 */
	var isDestroyed = false
		private set

	/** Calculates entity motion. */
	open fun calculateMotion() {
		rotateDeg += rotation
		if (rotateDeg < 0) {
			rotateDeg += 360
		} else if (rotateDeg > 359) {
			rotateDeg -= 360
		}
		val radians = Math.toRadians(rotateDeg.toDouble())
		val speedMultiplier = 0.1f
		val accel = if (isAccelerating) acceleration else 0
		velX += (sin(radians) * accel * speedMultiplier).toFloat()
		velY += (cos(radians) * accel * speedMultiplier).toFloat()
		pos.x += velX * speedMultiplier
		pos.y -= velY * speedMultiplier
		wrapScreen()
	}

	/** Wraps entity from one side of the screen to the opposite side. */
	private fun wrapScreen() {
		if (pos.x < 0) {
			pos.x += GameView.VIEW_WIDTH.toFloat()
		} else if (pos.x > GameView.VIEW_WIDTH) {
			pos.x -= GameView.VIEW_WIDTH.toFloat()
		}
		if (pos.y < 0) {
			pos.y += GameView.VIEW_HEIGHT.toFloat()
		} else if (pos.y > GameView.VIEW_HEIGHT) {
			pos.y -= GameView.VIEW_HEIGHT.toFloat()
		}
	}

	/**
	 * Rotates entity left at the predefined speed.
	 * @see rotateRight
	 * @see rotateStop
	 */
	fun rotateLeft() {
		rotation = -rotationSpeed
	}

	/**
	 * Rotates entity right at the predefined speed.
	 * @see rotateLeft
	 * @see rotateStop
	 */
	fun rotateRight() {
		rotation = rotationSpeed
	}

	/**
	 * Stops entity rotation.
	 * @see rotateLeft
	 * @see rotateRight
	 */
	fun rotateStop() {
		rotation = 0
	}

	/**
	 * Checks if this entity is contacting another. This is not the same as a collision and does not modify either object.
	 * @param otherEntity The other entity to test against.
	 * @return True if this and otherEntity are contacting and neither are already destroyed.
	 * @see collide
	 */
	fun isContacting(otherEntity: Entity): Boolean {
		return !isDestroyed && !otherEntity.isDestroyed && (abs(pos.x - otherEntity.pos.x)
				+ abs(pos.y - otherEntity.pos.y) < radius + otherEntity.radius)
	}

	/**
	 * Collides this with another entity, destroying them.
	 * @param otherEntity The other entity to collide with.
	 */
	fun collide(otherEntity: Entity) {
		destroy()
		otherEntity.destroy()
	}

	/**
	 * Sets this entity as destroyed (not active).
	 * @see undestroy
	 * @see isDestroyed
	 */
	open fun destroy() {
		isDestroyed = true
	}

	/**
	 * Sets this entity as not destroyed (active).
	 * @see destroy
	 * @see isDestroyed
	 */
	fun undestroy() {
		isDestroyed = false
	}

	class Position internal constructor(var x: Float, var y: Float)
}
