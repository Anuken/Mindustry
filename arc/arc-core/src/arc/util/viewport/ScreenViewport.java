package arc.util.viewport;

import arc.graphics.Camera;

/**
 * A viewport where the world size is based on the size of the screen. By default 1 world unit == 1 screen pixel, but this ratio
 * can be {@link #setUnitsPerPixel(float) changed}.
 * @author Daniel Holderbaum
 * @author Nathan Sweet
 */
public class ScreenViewport extends Viewport{
    private float unitsPerPixel = 1;

    /** Creates a new viewport using a new {@link Camera}. */
    public ScreenViewport(){
        this(new Camera());
    }

    public ScreenViewport(Camera camera){
        setCamera(camera);
    }

    @Override
    public void update(int screenWidth, int screenHeight, boolean centerCamera){
        setScreenBounds(0, 0, screenWidth, screenHeight);
        setWorldSize(screenWidth * unitsPerPixel, screenHeight * unitsPerPixel);
        apply(centerCamera);
    }

    public float getUnitsPerPixel(){
        return unitsPerPixel;
    }

    /**
     * Sets the number of pixels for each world unit. Eg, a scale of 2.5 means there are 2.5 world units for every 1 screen pixel.
     * Default is 1.
     */
    public void setUnitsPerPixel(float unitsPerPixel){
        this.unitsPerPixel = unitsPerPixel;
    }
}
