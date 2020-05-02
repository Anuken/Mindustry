package mindustry.ai.ai.utils;


import arc.math.geom.*;

/**
 * A {@code Collision} is made up of a collision point and the normal at that point of collision.
 * @author davebaol
 */
public class Collision{
    /** The collision point. */
    public Vec2 point;
    /** The normal of this collision. */
    public Vec2 normal;

    /**
     * Creates a {@code Collision} with the given {@code point} and {@code normal}.
     * @param point the point where this collision occurred
     * @param normal the normal of this collision
     */
    public Collision(Vec2 point, Vec2 normal){
        this.point = point;
        this.normal = normal;
    }

    /**
     * Sets this collision from the given collision.
     * @param collision The collision
     * @return this collision for chaining.
     */
    public Collision set(Collision collision){
        this.point.set(collision.point);
        this.normal.set(collision.normal);
        return this;
    }

    /**
     * Sets this collision from the given point and normal.
     * @param point the collision point
     * @param normal the normal of this collision
     * @return this collision for chaining.
     */
    public Collision set(Vec2 point, Vec2 normal){
        this.point.set(point);
        this.normal.set(normal);
        return this;
    }
}
