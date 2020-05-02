package mindustry.ai.ai.utils;


import arc.math.geom.*;

/**
 * A {@code Ray} is made up of a starting point and an ending point.
 * @author davebaol
 */
public class Ray{
    /** The starting point of this ray. */
    public Vec2 start;
    /** The ending point of this ray. */
    public Vec2 end;

    /**
     * Creates a {@code Ray} with the given {@code start} and {@code end} points.
     * @param start the starting point of this ray
     * @param end the starting point of this ray
     */
    public Ray(Vec2 start, Vec2 end){
        this.start = start;
        this.end = end;
    }

    /**
     * Sets this ray from the given ray.
     * @param ray The ray
     * @return this ray for chaining.
     */
    public Ray set(Ray ray){
        start.set(ray.start);
        end.set(ray.end);
        return this;
    }

    /**
     * Sets this Ray from the given start and end points.
     * @param start the starting point of this ray
     * @param end the starting point of this ray
     * @return this ray for chaining.
     */
    public Ray set(Vec2 start, Vec2 end){
        this.start.set(start);
        this.end.set(end);
        return this;
    }
}
