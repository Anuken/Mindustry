package arc.graphics;

import arc.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;

public class Camera{
    /** temporary vector which is returned. */
    private static final Vec2 tmpVector = new Vec2();
    /** the position of the camera **/
    public final Vec2 position = new Vec2();
    /** the view matrix**/
    public final Mat mat = new Mat();
    /** the inverse view matrix **/
    public final Mat inv = new Mat();
    /** the viewport width and height **/
    public float width, height;

    /**
     * Recalculates the projection and view matrix of this camera. Use this after you've manipulated
     * any of the attributes of the camera.
     */
    public void update(){
        mat.setOrtho(position.x - width / 2f, position.y - height / 2f, width, height);
        inv.set(mat).inv();
    }

    public void resize(float viewportWidth, float viewportHeight){
        this.width = viewportWidth;
        this.height = viewportHeight;
        update();
    }

    /**
     * Function to translate a point given in screen coordinates to world space. It's the same as GLU gluUnProject, but does not
     * rely on OpenGL. The x- and y-coordinate of vec are assumed to be in screen coordinates (origin is the bottom left corner, y
     * pointing up, x pointing to the right) as reported by the touch methods in {@link Input}. A z-coordinate of 0 will return a
     * point on the near plane, a z-coordinate of 1 will return a point on the far plane. This method allows you to specify the
     * viewport position and dimensions in the coordinate system expected by {@link Gl#viewport(int, int, int, int)}, with the
     * origin in the bottom left corner of the screen.
     * @param screenCoords the point in screen coordinates (origin top left)
     * @param viewportX the coordinate of the bottom left corner of the viewport in glViewport coordinates.
     * @param viewportY the coordinate of the bottom left corner of the viewport in glViewport coordinates.
     * @param viewportWidth the width of the viewport in pixels
     * @param viewportHeight the height of the viewport in pixels
     * @return the mutated and unprojected screenCoords {@link Vec3}
     */
    public Vec2 unproject(Vec2 screenCoords, float viewportX, float viewportY, float viewportWidth, float viewportHeight){
        float x = screenCoords.x - viewportX, y = screenCoords.y - viewportY;
        screenCoords.x = (2 * x) / viewportWidth - 1;
        screenCoords.y = (2 * y) / viewportHeight - 1;
        screenCoords.mul(inv);
        return screenCoords;
    }

    /**
     * Function to translate a point given in screen coordinates to world space. It's the same as GLU gluUnProject but does not
     * rely on OpenGL. The viewport is assumed to span the whole screen and is fetched from {@link Graphics#getWidth()} and
     * {@link Graphics#getHeight()}. The x- and y-coordinate of vec are assumed to be in screen coordinates (origin is the bottom left
     * corner, y pointing up, x pointing to the right) as reported by the touch methods in {@link Input}. A z-coordinate of 0
     * will return a point on the near plane, a z-coordinate of 1 will return a point on the far plane.
     * @param screenCoords the point in screen coordinates
     * @return the mutated and unprojected screenCoords {@link Vec3}
     */
    public Vec2 unproject(Vec2 screenCoords){
        unproject(screenCoords, 0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
        return screenCoords;
    }

    /** See {@link #unproject(Vec2)}. Returns the same Vec2 each time. */
    public Vec2 unproject(float screenX, float screenY){
        unproject(tmpVector.set(screenX, screenY), 0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
        return tmpVector;
    }

    /**
     * Projects the {@link Vec3} given in world space to screen coordinates. It's the same as GLU gluProject with one small
     * deviation: The viewport is assumed to span the whole screen. The screen coordinate system has its origin in the
     * <b>bottom</b> left, with the y-axis pointing <b>upwards</b> and the x-axis pointing to the right.
     * @return the mutated and projected worldCoords {@link Vec3}
     */
    public Vec2 project(Vec2 worldCoords){
        project(worldCoords, 0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
        return worldCoords;
    }

    /** See {@link #project(Vec2)}. Returns the same Vec2 each time. */
    public Vec2 project(float screenX, float screenY){
        project(tmpVector.set(screenX, screenY), 0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
        return tmpVector;
    }

    /**
     * Projects the {@link Vec3} given in world space to screen coordinates. It's the same as GLU gluProject with one small
     * deviation: The viewport is assumed to span the whole screen. The screen coordinate system has its origin in the
     * <b>bottom</b> left, with the y-axis pointing <b>upwards</b> and the x-axis pointing to the right.
     * This method allows you to specify the viewport position and
     * dimensions in the coordinate system expected by {@link Gl#viewport(int, int, int, int)}, with the origin in the bottom
     * left corner of the screen.
     * @param viewportX the coordinate of the bottom left corner of the viewport in glViewport coordinates.
     * @param viewportY the coordinate of the bottom left corner of the viewport in glViewport coordinates.
     * @param viewportWidth the width of the viewport in pixels
     * @param viewportHeight the height of the viewport in pixels
     * @return the mutated and projected worldCoords {@link Vec3}
     */
    public Vec2 project(Vec2 worldCoords, float viewportX, float viewportY, float viewportWidth, float viewportHeight){
        worldCoords.mul(mat);
        worldCoords.x = viewportWidth * (worldCoords.x + 1) / 2 + viewportX;
        worldCoords.y = viewportHeight * (worldCoords.y + 1) / 2 + viewportY;
        return worldCoords;
    }

    /** Sets the specified rectangle to this camera's bounds.*/
    public Rect bounds(Rect out){
        return out.setSize(width, height).setCenter(position);
    }
}
