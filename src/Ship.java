import java.awt.Image;
import java.awt.geom.AffineTransform;


public class Ship {
	private static final int THRUST = 1, ROTATE_SPEED = 1; 
	private Image image;
	private float posX, posY, velX, velY;
	private int shipCenter, thrust, rotateSpeed, rotateDeg;
	
	/**
	 * Creates a new ship.
	 * @param img Image to use for the ship.
	 * @param x Initial x-position.
	 * @param y Initial y-position.
	 */
	Ship(Image img, int x, int y) {
		image = img;
		shipCenter = image.getWidth(null) / 2;
		posX = x;
		posY = y;
	}
	
	Image getImage() {
		return image;
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
	
	private AffineTransform trans = new AffineTransform();
	AffineTransform getTransform() {
		trans.setToTranslation(posX - shipCenter, posY - shipCenter);
		trans.rotate(Math.toRadians(rotateDeg), shipCenter, shipCenter);
		return trans;
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
