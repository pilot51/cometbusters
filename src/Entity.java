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

public abstract class Entity {
	protected final Position pos;
	protected float velX, velY;
	protected int radius;
	protected boolean isAccelerating;
	/** Initial direction of travel in degrees. */
	protected final int direction;
	/** Initial velocity. */
	protected final int velocity;
	/** Current rotation in degrees. */
	protected int rotateDeg;
	/** Current rotation direction and speed. Positive is clockwise, negative is counter-clockwise. */
	private int rotation;
	/** Forward acceleration applied when {@link #isAccelerating} is true. */
	private final int ACCELERATION;
	/** Rotation speed applied when {@link #rotateLeft()} or {@link #rotateRight()} is called, stopped with {@link #rotateStop()}. */
	private final int ROTATE_SPEED;
	private boolean isDestroyed;

	/**
	 * Creates an object at the given coordinates with initial rotation and velocity.
	 * @param x Initial x-position.
	 * @param y Initial y-position.
	 * @param direction Initial direction of travel in degrees.
	 * @param velocity Initial forward velocity.
	 */
	Entity(float x, float y, int direction, int velocity) {
		this(x, y, direction, velocity, 0, 0);
	}

	/**
	 * Creates an object at the given coordinates with initial rotation, initial velocity,
	 * potential acceleration, and potential rotation speed.
	 * @param x Initial x-position.
	 * @param y Initial y-position.
	 * @param direction Initial direction of travel in degrees.
	 * @param velocity Initial forward velocity.
	 * @param acceleration Forward acceleration applied when {@link #isAccelerating} is true.
	 * @param rotationSpeed Rotation speed applied when {@link #rotateLeft()} or {@link #rotateRight()} is called, stopped with {@link #rotateStop()}.
	 */
	Entity(float x, float y, int direction, int velocity, int acceleration, int rotationSpeed) {
		pos = new Position(x, y);
		ACCELERATION = acceleration;
		ROTATE_SPEED = rotationSpeed;
		this.direction = direction;
		this.velocity = velocity;
		final double radians = Math.toRadians(direction);
		velX = (float)Math.sin(radians) * velocity;
		velY = (float)Math.cos(radians) * velocity;
	}

	final int getRadius() {
		return radius;
	}

	/**
	 * Calculates entity motion.
	 */
	void calculateMotion() {
		rotateDeg += rotation;
		if (rotateDeg < 0) {
			rotateDeg += 360;
		} else if (rotateDeg > 359) {
			rotateDeg -= 360;
		}
		final double radians = Math.toRadians(rotateDeg);
		final float speedMultiplier = 0.1f;
		int accel = isAccelerating ? ACCELERATION : 0;
		velX += Math.sin(radians) * accel * speedMultiplier;
		velY += Math.cos(radians) * accel * speedMultiplier;
		pos.x += velX * speedMultiplier;
		pos.y -= velY * speedMultiplier;
		wrapScreen();
	}

	/**
	 * Wraps entity from one side of the screen to the opposite side.
	 */
	private void wrapScreen() {
		if (pos.x < 0) {
			pos.x += GameView.VIEW_WIDTH;
		} else if (pos.x > GameView.VIEW_WIDTH) {
			pos.x -= GameView.VIEW_WIDTH;
		}
		if (pos.y < 0) {
			pos.y += GameView.VIEW_HEIGHT;
		} else if (pos.y > GameView.VIEW_HEIGHT) {
			pos.y -= GameView.VIEW_HEIGHT;
		}
	}

	/**
	 * Sets entity to accelerate at the predefined rate.
	 * @param accel True to accelerate, false to stop accelerating.
	 */
	void setAccelerating(boolean accel) {
		isAccelerating = accel;
	}

	/**
	 * Rotates entity left at the predefined speed.
	 * @see #rotateRight()
	 * @see #rotateStop()
	 */
	void rotateLeft() {
		rotation = -ROTATE_SPEED;
	}

	/**
	 * Rotates entity right at the predefined speed.
	 * @see #rotateLeft()
	 * @see #rotateStop()
	 */
	void rotateRight() {
		rotation = ROTATE_SPEED;
	}

	/**
	 * Stops entity rotation.
	 * @see #rotateLeft()
	 * @see #rotateRight()
	 */
	void rotateStop() {
		rotation = 0;
	}

	/**
	 * Checks if this entity is contacting another. This is not the same as a collision and does not modify either object.
	 * @param otherEntity The other entity to test against.
	 * @return True if this and otherEntity are contacting and neither are already destroyed.
	 * @see #collide(Entity)
	 */
	final boolean isContacting(Entity otherEntity) {
		return !isDestroyed && !otherEntity.isDestroyed && Math.abs(pos.x - otherEntity.pos.x)
				+ Math.abs(pos.y - otherEntity.pos.y) < radius + otherEntity.radius;
	}

	/**
	 * Collides this with another entity, destroying them.
	 * @param otherEntity The other entity to collide with.
	 */
	void collide(Entity otherEntity) {
		destroy();
		otherEntity.destroy();
	}

	/**
	 * Sets this entity as destroyed (not active).
	 * @see #undestroy()
	 * @see #isDestroyed()
	 */
	void destroy() {
		isDestroyed = true;
	}

	/**
	 * Sets this entity as not destroyed (active).
	 * @see #destroy()
	 * @see #isDestroyed()
	 */
	void undestroy() {
		isDestroyed = false;
	}

	/**
	 * @return True if this entity is destroyed (not active), false if not.
	 * @see #destroy()
	 * @see #undestroy()
	 */
	final boolean isDestroyed() {
		return isDestroyed;
	}

	public static class Position {
		float x, y;
		Position(float x, float y) {
			this.x = x;
			this.y = y;
		}
	}
}
