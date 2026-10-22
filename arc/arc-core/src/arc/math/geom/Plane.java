package arc.math.geom;

/**
 * A plane defined via a unit length normal and the distance from the origin, as you learned in your math class.
 * @author badlogicgames@gmail.com
 */
public class Plane{
    public final Vec3 normal = new Vec3();
    public float d = 0;

    /**
     * Constructs a new plane with all values set to 0
     */
    public Plane(){

    }

    /**
     * Constructs a new plane based on the normal and distance to the origin.
     * @param normal The plane normal
     * @param d The distance to the origin
     */
    public Plane(Vec3 normal, float d){
        this.normal.set(normal).nor();
        this.d = d;
    }

    /**
     * Constructs a new plane based on the normal and a point on the plane.
     * @param normal The normal
     * @param point The point on the plane
     */
    public Plane(Vec3 normal, Vec3 point){
        this.normal.set(normal).nor();
        this.d = -this.normal.dot(point);
    }

    /**
     * Constructs a new plane out of the three given points that are considered to be on the plane. The normal is calculated via a
     * cross product between (point1-point2)x(point2-point3)
     * @param point1 The first point
     * @param point2 The second point
     * @param point3 The third point
     */
    public Plane(Vec3 point1, Vec3 point2, Vec3 point3){
        set(point1, point2, point3);
    }

    /**
     * Sets the plane normal and distance to the origin based on the three given points which are considered to be on the plane.
     * The normal is calculated via a cross product between (point1-point2)x(point2-point3)
     */
    public void set(Vec3 point1, Vec3 point2, Vec3 point3){
        normal.set(point1).sub(point2).crs(point2.x - point3.x, point2.y - point3.y, point2.z - point3.z).nor();
        d = -point1.dot(normal);
    }

    /**
     * Sets the plane normal and distance
     * @param nx normal x-component
     * @param ny normal y-component
     * @param nz normal z-component
     * @param d distance to origin
     */
    public void set(float nx, float ny, float nz, float d){
        normal.set(nx, ny, nz);
        this.d = d;
    }

    /** Projects the supplied vector onto this plane.
     * @param v the vector to project onto this plane. */
    public Vec3 project(Vec3 v){
        float npd = normal.dot(v) + d;
        return v.sub(npd * normal.x, npd * normal.y, npd * normal.z);
    }

    /**
     * Calculates the shortest signed distance between the plane and the given point.
     * @param point The point
     * @return the shortest signed distance between the plane and the point
     */
    public float distance(Vec3 point){
        return normal.dot(point) + d;
    }

    /**
     * Returns on which side the given point lies relative to the plane and its normal. PlaneSide.Front refers to the side the
     * plane normal points to.
     * @param point The point
     * @return The side the point lies relative to the plane
     */
    public PlaneSide testPoint(Vec3 point){
        float dist = normal.dot(point) + d;

        if(dist == 0)
            return PlaneSide.onPlane;
        else if(dist < 0)
            return PlaneSide.back;
        else
            return PlaneSide.front;
    }

    /**
     * Returns on which side the given point lies relative to the plane and its normal. PlaneSide.Front refers to the side the
     * plane normal points to.
     * @return The side the point lies relative to the plane
     */
    public PlaneSide testPoint(float x, float y, float z){
        float dist = normal.dot(x, y, z) + d;

        if(dist == 0)
            return PlaneSide.onPlane;
        else if(dist < 0)
            return PlaneSide.back;
        else
            return PlaneSide.front;
    }

    /**
     * Returns whether the plane is facing the direction vector. Think of the direction vector as the direction a camera looks in.
     * This method will return true if the front side of the plane determined by its normal faces the camera.
     * @param direction the direction
     * @return whether the plane is front facing
     */
    public boolean isFrontFacing(Vec3 direction){
        float dot = normal.dot(direction);
        return dot <= 0;
    }

    /** @return The normal */
    public Vec3 getNormal(){
        return normal;
    }

    /** @return The distance to the origin */
    public float getD(){
        return d;
    }

    /**
     * Sets the plane to the given point and normal.
     * @param point the point on the plane
     * @param normal the normal of the plane
     */
    public void set(Vec3 point, Vec3 normal){
        this.normal.set(normal);
        d = -point.dot(normal);
    }

    public void set(float pointX, float pointY, float pointZ, float norX, float norY, float norZ){
        this.normal.set(norX, norY, norZ);
        d = -(pointX * norX + pointY * norY + pointZ * norZ);
    }

    /**
     * Sets this plane from the given plane
     * @param plane the plane
     */
    public void set(Plane plane){
        this.normal.set(plane.normal);
        this.d = plane.d;
    }

    @Override
    public String toString(){
        return normal.toString() + ", " + d;
    }

    /**
     * Enum specifying on which side a point lies respective to the plane and it's normal. {@link PlaneSide#front} is the side to
     * which the normal points.
     * @author mzechner
     */
    public enum PlaneSide{
        onPlane, back, front
    }
}
