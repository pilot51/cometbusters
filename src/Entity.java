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

public class Entity {
	protected float posX, posY, velX, velY;
	protected boolean isAccelerating;
	/** Current rotation in degrees. */
	protected int rotateDeg;
	/** Current rotation direction and speed. Positive is clockwise, negative is counter-clockwise. */
	private int rotation;
	/** Forward acceleration applied when {@link #isAccelerating} is true. */
	private final int ACCELERATION;
	/** Rotation speed applied when {@link #rotateLeft()} or {@link #rotateRight()} is called, stopped with {@link #rotateStop()}. */
	private final int ROTATE_SPEED;
	
	/**
	 * Creates an object at the given coordinates with potential acceleration and rotation speed.
	 * @param x Initial x-position.
	 * @param y Initial y-position.
	 * @param acceleration Forward acceleration applied when {@link #isAccelerating} is true.
	 * @param rotationSpeed Rotation speed applied when {@link #rotateLeft()} or {@link #rotateRight()} is called, stopped with {@link #rotateStop()}.
	 */
	Entity(int x, int y, int acceleration, int rotationSpeed) {
		this(x, y, 0, 0, acceleration, rotationSpeed);
	}
	
	/**
	 * Creates an object at the given coordinates with initial rotation, initial velocity,
	 * potential acceleration, and potential rotation speed.
	 * @param x Initial x-position.
	 * @param y Initial y-position.
	 * @param rotationDeg Initial rotation in degrees.
	 * @param velocity Initial forward velocity.
	 * @param acceleration Forward acceleration applied when {@link #isAccelerating} is true.
	 * @param rotationSpeed Rotation speed applied when {@link #rotateLeft()} or {@link #rotateRight()} is called, stopped with {@link #rotateStop()}.
	 */
	Entity(float x, float y, int rotationDeg, int velocity, int acceleration, int rotationSpeed) {
		posX = x;
		posY = y;
		ACCELERATION = acceleration;
		ROTATE_SPEED = rotationSpeed;
		final double radians = Math.toRadians(rotationDeg);
		velX = (float)Math.sin(radians) * velocity;
		velY = (float)Math.cos(radians) * velocity;
	}
	
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
		posX += velX * speedMultiplier;
		posY -= velY * speedMultiplier;
		wrapScreen();
	}
	
	private void wrapScreen() {
		if (posX < 0) {
			posX += GameView.VIEW_WIDTH;
		} else if (posX > GameView.VIEW_WIDTH) {
			posX -= GameView.VIEW_WIDTH;
		}
		if (posY < 0) {
			posY += GameView.VIEW_HEIGHT;
		} else if (posY > GameView.VIEW_HEIGHT) {
			posY -= GameView.VIEW_HEIGHT;
		}
	}
	
	void setAccelerating(boolean accel) {
		isAccelerating = accel;
	}
	
	void rotateLeft() {
		rotation = -ROTATE_SPEED;
	}
	
	void rotateRight() {
		rotation = ROTATE_SPEED;
	}
	
	void rotateStop() {
		rotation = 0;
	}
}
