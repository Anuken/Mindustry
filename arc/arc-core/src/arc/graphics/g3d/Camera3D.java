package arc.graphics.g3d;

import arc.*;
import arc.math.*;
import arc.math.geom.*;

public class Camera3D{
    /** field of view. */
    public float fov = 67;
    /** the near clipping plane distance, has to be positive **/
    public float near = 1;
    /** the far clipping plane distance, has to be positive **/
    public float far = 100;
    /** if set to true, a perspective projection is used. */
    public boolean perspective = true;
    /** the viewport width and height **/
    public float width, height;
    /** the position of the camera **/
    public final Vec3 position = new Vec3();
    /** the unit length direction vector of the camera **/
    public final Vec3 direction = new Vec3(0, 0, -1);
    /** the unit length up vector of the camera **/
    public final Vec3 up = new Vec3(0, 1, 0);

    /** the combined projection and view matrix **/
    public final Mat3D combined = new Mat3D();
    /** the projection matrix **/
    public final Mat3D projection = new Mat3D();
    /** the view matrix **/
    public final Mat3D view = new Mat3D();
    /** the inverse combined projection and view matrix **/
    public final Mat3D invProjectionView = new Mat3D();

    /** the frustum, for clipping operations **/
    public final Frustum frustum = new Frustum();

    private final Vec3 tmpVec = new Vec3();
    private final Ray ray = new Ray(new Vec3(), new Vec3());

    public void update(){
        if(perspective){
            projection.setToProjection(Math.abs(near), Math.abs(far), fov, width / height);
        }else{
            projection.setToOrtho(-width / 2, width / 2, -height / 2, height / 2, near, far);
        }

        view.setToLookAt(position, tmpVec.set(position).add(direction), up);
        combined.set(projection).mul(view);
        invProjectionView.set(combined).inv();
        frustum.update(invProjectionView);
    }

    public void resize(float width, float height){
        this.width = width;
        this.height = height;
    }

    public void lookAt(float x, float y, float z){
        tmpVec.set(x, y, z).sub(position).nor();
        if(!tmpVec.isZero()){
            float dot = tmpVec.dot(up); // up and direction must ALWAYS be orthonormal vectors
            if(Math.abs(dot - 1) < 0.000000001f){
                // Collinear
                up.set(direction).scl(-1);
            }else if(Math.abs(dot + 1) < 0.000000001f){
                // Collinear opposite
                up.set(direction);
            }
            direction.set(tmpVec);
            normalizeUp();
        }
    }

    /**
     * Recalculates the direction of the camera to look at the point (x, y, z).
     * @param target the point to look at
     */
    public void lookAt(Vec3 target){
        lookAt(target.x, target.y, target.z);
    }

    /**
     * Normalizes the up vector by first calculating the right vector via a cross product between direction and up, and then
     * recalculating the up vector via a cross product between right and direction.
     */
    public void normalizeUp(){
        tmpVec.set(direction).crs(up);
        up.set(tmpVec).crs(direction).nor();
    }

    /**
     * Function to translate a point given in screen coordinates to world space. It's the same as GLU gluUnProject, but does not
     * rely on OpenGL. The x- and y-coordinate of vec are assumed to be in screen coordinates (origin is the top left corner, y
     * pointing down, x pointing to the right) as reported by the touch methods in {@link Input}. A z-coordinate of 0 will return a
     * point on the near plane, a z-coordinate of 1 will return a point on the far plane. This method allows you to specify the
     * viewport position and dimensions in the coordinate system expected by glViewport(int, int, int, int), with the
     * origin in the bottom left corner of the screen.
     * @param screenCoords the point in screen coordinates (origin top left)
     * @param viewportX the coordinate of the bottom left corner of the viewport in glViewport coordinates.
     * @param viewportY the coordinate of the bottom left corner of the viewport in glViewport coordinates.
     * @param viewportWidth the width of the viewport in pixels
     * @param viewportHeight the height of the viewport in pixels
     * @return the mutated and unprojected screenCoords {@link Vec3}
     */
    public Vec3 unproject(Vec3 screenCoords, float viewportX, float viewportY, float viewportWidth, float viewportHeight){
        float x = screenCoords.x, y = screenCoords.y;
        x = x - viewportX;
        y = y - viewportY;
        screenCoords.x = (2 * x) / viewportWidth - 1;
        screenCoords.y = (2 * y) / viewportHeight - 1;
        screenCoords.z = 2 * screenCoords.z - 1;
        Mat3D.prj(screenCoords, invProjectionView);
        return screenCoords;
    }

    /**
     * Function to translate a point given in screen coordinates to world space. It's the same as GLU gluUnProject but does not
     * rely on OpenGL. The viewport is assumed to span the whole screen and is fetched from {@link Graphics#getWidth()} and
     * {@link Graphics#getHeight()}. The x- and y-coordinate of vec are assumed to be in screen coordinates (origin is the top left
     * corner, y pointing down, x pointing to the right) as reported by the touch methods in {@link Input}. A z-coordinate of 0
     * will return a point on the near plane, a z-coordinate of 1 will return a point on the far plane.
     * @param screenCoords the point in screen coordinates
     * @return the mutated and unprojected screenCoords {@link Vec3}
     */
    public Vec3 unproject(Vec3 screenCoords){
        unproject(screenCoords, 0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
        return screenCoords;
    }

    /**
     * Projects the {@link Vec3} given in world space to screen coordinates. It's the same as GLU gluProject with one small
     * deviation: The viewport is assumed to span the whole screen. The screen coordinate system has its origin in the
     * <b>bottom</b> left, with the y-axis pointing <b>upwards</b> and the x-axis pointing to the right. This makes it easily
     * useable in conjunction with Batch and similar classes.
     * @return the mutated and projected worldCoords {@link Vec3}
     */
    public Vec3 project(Vec3 worldCoords){
        project(worldCoords, 0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
        return worldCoords;
    }

    /**
     * Projects the {@link Vec3} given in world space to screen coordinates. It's the same as GLU gluProject with one small
     * deviation: The viewport is assumed to span the whole screen. The screen coordinate system has its origin in the
     * <b>bottom</b> left, with the y-axis pointing <b>upwards</b> and the x-axis pointing to the right. This makes it easily
     * useable in conjunction with Batch and similar classes. This method allows you to specify the viewport position and
     * dimensions in the coordinate system expected by glViewport(int, int, int, int), with the origin in the bottom
     * left corner of the screen.
     * @param viewportX the coordinate of the bottom left corner of the viewport in glViewport coordinates.
     * @param viewportY the coordinate of the bottom left corner of the viewport in glViewport coordinates.
     * @param viewportWidth the width of the viewport in pixels
     * @param viewportHeight the height of the viewport in pixels
     * @return the mutated and projected worldCoords {@link Vec3}
     */
    public Vec3 project(Vec3 worldCoords, float viewportX, float viewportY, float viewportWidth, float viewportHeight){
        Mat3D.prj(worldCoords, combined);
        worldCoords.x = viewportWidth * (worldCoords.x + 1) / 2 + viewportX;
        worldCoords.y = viewportHeight * (worldCoords.y + 1) / 2 + viewportY;
        worldCoords.z = (worldCoords.z + 1) / 2;
        return worldCoords;
    }

    public Ray getMouseRay(){
        return getPickRay(Core.input.mouseX(), Core.input.mouseY());
    }

    /**
     * Creates a picking {@link Ray} from the coordinates given in screen coordinates. It is assumed that the viewport spans the
     * whole screen. The screen coordinates origin is assumed to be in the top left corner, its y-axis pointing down, the x-axis
     * pointing to the right. The returned instance is not a new instance but an internal member only accessible via this function.
     * @param viewportX the coordinate of the bottom left corner of the viewport in glViewport coordinates.
     * @param viewportY the coordinate of the bottom left corner of the viewport in glViewport coordinates.
     * @param viewportWidth the width of the viewport in pixels
     * @param viewportHeight the height of the viewport in pixels
     * @return the picking Ray.
     */
    public Ray getPickRay(float screenX, float screenY, float viewportX, float viewportY, float viewportWidth, float viewportHeight){
        unproject(ray.origin.set(screenX, screenY, 0), viewportX, viewportY, viewportWidth, viewportHeight);
        unproject(ray.direction.set(screenX, screenY, 1), viewportX, viewportY, viewportWidth, viewportHeight);
        ray.direction.sub(ray.origin).nor();
        return ray;
    }

    /**
     * Creates a picking {@link Ray} from the coordinates given in screen coordinates. It is assumed that the viewport spans the
     * whole screen. The screen coordinates origin is assumed to be in the top left corner, its y-axis pointing down, the x-axis
     * pointing to the right. The returned instance is not a new instance but an internal member only accessible via this function.
     * @return the picking Ray.
     */
    public Ray getPickRay(float screenX, float screenY){
        return getPickRay(screenX, screenY, 0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
    }
}
