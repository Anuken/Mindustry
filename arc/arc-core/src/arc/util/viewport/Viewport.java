package arc.util.viewport;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;

/**
 * Manages a {@link Camera} and determines how world coordinates are mapped to and from the screen.
 * @author Daniel Holderbaum
 * @author Nathan Sweet
 */
public abstract class Viewport{
    private final Vec2 tmp = new Vec2();
    private Camera camera;
    private float worldWidth, worldHeight;
    private int screenX, screenY, screenWidth, screenHeight;

    /** Calls {@link #apply(boolean)} with false. */
    public void apply(){
        apply(false);
    }

    /**
     * Applies the viewport to the camera and sets the glViewport.
     * @param centerCamera If true, the camera position is set to the center of the world.
     */
    public void apply(boolean centerCamera){
        HdpiUtils.glViewport(screenX, screenY, screenWidth, screenHeight);
        camera.width = worldWidth;
        camera.height = worldHeight;
        if(centerCamera) camera.position.set(worldWidth / 2, worldHeight / 2);
        camera.update();
    }

    /** Calls {@link #update(int, int, boolean)} with false. */
    public final void update(int screenWidth, int screenHeight){
        update(screenWidth, screenHeight, false);
    }

    /**
     * Configures this viewport's screen bounds using the specified screen size and calls {@link #apply(boolean)}. Typically called
     * from {@link ApplicationListener#resize(int, int)}
     * <p>
     * The default implementation only calls {@link #apply(boolean)}.
     */
    public void update(int screenWidth, int screenHeight, boolean centerCamera){
        apply(centerCamera);
    }

    /**
     * Transforms the specified screen coordinate to world coordinates.
     * @return The vector that was passed in, transformed to world coordinates.
     * @see Camera#unproject(Vec2)
     */
    public Vec2 unproject(Vec2 screenCoords){
        tmp.set(screenCoords.x, screenCoords.y);
        camera.unproject(tmp, screenX, screenY, screenWidth, screenHeight);
        screenCoords.set(tmp.x, tmp.y);
        return screenCoords;
    }

    /**
     * Transforms the specified world coordinate to screen coordinates.
     * @return The vector that was passed in, transformed to screen coordinates.
     * @see Camera#project(Vec2)
     */
    public Vec2 project(Vec2 worldCoords){
        tmp.set(worldCoords.x, worldCoords.y);
        camera.project(tmp, screenX, screenY, screenWidth, screenHeight);
        worldCoords.set(tmp.x, tmp.y);
        return worldCoords;
    }

    /** @see ScissorStack#calculateScissors(Camera, float, float, float, float, Mat, Rect, Rect) */
    public void calculateScissors(Mat batchTransform, Rect area, Rect scissor){
        ScissorStack.calculateScissors(camera, screenX, screenY, screenWidth, screenHeight, batchTransform, area, scissor);
    }

    /**
     * Transforms a point to real screen coordinates (as opposed to OpenGL ES window coordinates), where the origin is in the top
     * left and the the y-axis is pointing downwards.
     */
    public Vec2 toScreenCoordinates(Vec2 worldCoords, Mat transformMatrix){
        tmp.set(worldCoords.x, worldCoords.y);
        tmp.mul(transformMatrix);
        camera.project(tmp);
        tmp.y = Core.graphics.getHeight() - tmp.y;
        worldCoords.x = tmp.x;
        worldCoords.y = tmp.y;
        return worldCoords;
    }

    public Camera getCamera(){
        return camera;
    }

    public void setCamera(Camera camera){
        this.camera = camera;
    }

    public float getWorldWidth(){
        return worldWidth;
    }

    /** The virtual width of this viewport in world coordinates. This width is scaled to the viewport's screen width. */
    public void setWorldWidth(float worldWidth){
        this.worldWidth = worldWidth;
    }

    public float getWorldHeight(){
        return worldHeight;
    }

    /** The virtual height of this viewport in world coordinates. This height is scaled to the viewport's screen height. */
    public void setWorldHeight(float worldHeight){
        this.worldHeight = worldHeight;
    }

    public void setWorldSize(float worldWidth, float worldHeight){
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
    }

    public int getScreenX(){
        return screenX;
    }

    /** Sets the viewport's offset from the left edge of the screen. This is typically set by {@link #update(int, int, boolean)}. */
    public void setScreenX(int screenX){
        this.screenX = screenX;
    }

    public int getScreenY(){
        return screenY;
    }

    /** Sets the viewport's offset from the bottom edge of the screen. This is typically set by {@link #update(int, int, boolean)}. */
    public void setScreenY(int screenY){
        this.screenY = screenY;
    }

    public int getScreenWidth(){
        return screenWidth;
    }

    /** Sets the viewport's width in screen coordinates. This is typically set by {@link #update(int, int, boolean)}. */
    public void setScreenWidth(int screenWidth){
        this.screenWidth = screenWidth;
    }

    public int getScreenHeight(){
        return screenHeight;
    }

    /** Sets the viewport's height in screen coordinates. This is typically set by {@link #update(int, int, boolean)}. */
    public void setScreenHeight(int screenHeight){
        this.screenHeight = screenHeight;
    }

    /** Sets the viewport's position in screen coordinates. This is typically set by {@link #update(int, int, boolean)}. */
    public void setScreenPosition(int screenX, int screenY){
        this.screenX = screenX;
        this.screenY = screenY;
    }

    /** Sets the viewport's size in screen coordinates. This is typically set by {@link #update(int, int, boolean)}. */
    public void setScreenSize(int screenWidth, int screenHeight){
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    /** Sets the viewport's bounds in screen coordinates. This is typically set by {@link #update(int, int, boolean)}. */
    public void setScreenBounds(int screenX, int screenY, int screenWidth, int screenHeight){
        this.screenX = screenX;
        this.screenY = screenY;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    /** Returns the left gutter (black bar) width in screen coordinates. */
    public int getLeftGutterWidth(){
        return screenX;
    }

    /** Returns the right gutter (black bar) x in screen coordinates. */
    public int getRightGutterX(){
        return screenX + screenWidth;
    }

    /** Returns the right gutter (black bar) width in screen coordinates. */
    public int getRightGutterWidth(){
        return Core.graphics.getWidth() - (screenX + screenWidth);
    }

    /** Returns the bottom gutter (black bar) height in screen coordinates. */
    public int getBottomGutterHeight(){
        return screenY;
    }

    /** Returns the top gutter (black bar) y in screen coordinates. */
    public int getTopGutterY(){
        return screenY + screenHeight;
    }

    /** Returns the top gutter (black bar) height in screen coordinates. */
    public int getTopGutterHeight(){
        return Core.graphics.getHeight() - (screenY + screenHeight);
    }
}
