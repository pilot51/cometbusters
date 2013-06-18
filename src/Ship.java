import java.awt.Image;
import java.awt.geom.AffineTransform;


public class Ship {
	private static final int THRUST = 1, ROTATE_SPEED = 1; 
	private Image image;
	private Image[] thrustImages;
	private float posX, posY, velX, velY;
	private int shipRadius, thrustRadius, thrust, rotateSpeed, rotateDeg;
	private AffineTransform trans = new AffineTransform();
	
	/**
	 * Creates a new ship.
	 * @param img Image to use for the ship.
	 * @param thrustImgs Array of thrust images to be animated.
	 * @param x Initial x-position.
	 * @param y Initial y-position.
	 */
	Ship(Image img, Image[] thrustImgs, int x, int y) {
		image = img;
		shipRadius = image.getWidth(null) / 2;
		thrustImages = thrustImgs;
		thrustRadius = thrustImages[0].getWidth(null) / 2;
		posX = x;
		posY = y;
	}
	
	Image getImage() {
		return image;
	}
	
	private int thrustFrame;
	Image getThrustImage() {
		if (++thrustFrame == thrustImages.length) {
			thrustFrame = 0;
		}
		return thrustImages[thrustFrame];
	}
	
	void calculateMotion() {
		rotateDeg += rotateSpeed;
		if (rotateDeg < 0) {
			rotateDeg += 360;
		} else if (rotateDeg > 359) {
			rotateDeg -= 360;
		}
		final double radians = Math.toRadians(rotateDeg);
		final float dt = 0.1f;
		velY -= Math.cos(radians) * thrust * dt;
		velX += Math.sin(radians) * thrust * dt;
		posY += velY * dt;
		posX += velX * dt;
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
	
	AffineTransform getTransform() {
		trans.setToTranslation(posX - shipRadius, posY - shipRadius);
		trans.rotate(Math.toRadians(rotateDeg), shipRadius, shipRadius);
		return trans;
	}
	
	AffineTransform getThrustTransform() {
		trans.setToTranslation(posX - (thrustRadius - 1), posY + shipRadius / 2);
		trans.rotate(Math.toRadians(rotateDeg), thrustRadius - 1, -shipRadius / 2);
		return trans;
	}
	
	boolean isThrustActive() {
		return thrust > 0;
	}
	
	void thrust(boolean activate) {
		thrust = activate ? THRUST : 0;
	}
	
	void rotateLeft() {
		rotateSpeed = -ROTATE_SPEED;
	}
	
	void rotateRight() {
		rotateSpeed = ROTATE_SPEED;
	}
	
	void rotateStop() {
		rotateSpeed = 0;
	}
}
