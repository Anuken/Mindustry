package arc.backend.robovm;

public final class IOSScreenBounds {

	/** Offset from top left corner in points */
	public final int x, y;

	/** Dimensions of drawing surface in points */
	public final int width, height;

	/** Dimensions of drawing surface in pixels */
	public final int backBufferWidth, backBufferHeight;

	public IOSScreenBounds (int x, int y, int width, int height, int backBufferWidth, int backBufferHeight) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.backBufferWidth = backBufferWidth;
		this.backBufferHeight = backBufferHeight;
	}
}