package arc.math.geom;

import arc.math.*;

/**
 * A convenient 2D circle class.
 * @author mzechner
 */
public class Circle implements Shape2D{
    public float x, y;
    public float radius;

    /** Constructs a new circle with all values set to zero */
    public Circle(){

    }

    /**
     * Constructs a new circle with the given X and Y coordinates and the given radius.
     * @param x X coordinate
     * @param y Y coordinate
     * @param radius The radius of the circle
     */
    public Circle(float x, float y, float radius){
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    /**
     * Constructs a new circle using a given {@link Vec2} that contains the desired X and Y coordinates, and a given radius.
     * @param position The position {@link Vec2}.
     * @param radius The radius
     */
    public Circle(Vec2 position, float radius){
        this.x = position.x;
        this.y = position.y;
        this.radius = radius;
    }

    /**
     * Copy constructor
     * @param circle The circle to construct a copy of.
     */
    public Circle(Circle circle){
        this.x = circle.x;
        this.y = circle.y;
        this.radius = circle.radius;
    }

    /**
     * Creates a new {@link Circle} in terms of its center and a point on its edge.
     * @param center The center of the new circle
     * @param edge Any point on the edge of the given circle
     */
    public Circle(Vec2 center, Vec2 edge){
        this.x = center.x;
        this.y = center.y;
        this.radius = Mathf.len(center.x - edge.x, center.y - edge.y);
    }

    /**
     * Sets a new location and radius for this circle.
     * @param x X coordinate
     * @param y Y coordinate
     * @param radius Circle radius
     */
    public Circle set(float x, float y, float radius){
        this.x = x;
        this.y = y;
        this.radius = radius;
        return this;
    }

    /**
     * Sets a new location and radius for this circle.
     * @param position Position {@link Vec2} for this circle.
     * @param radius Circle radius
     */
    public Circle set(Vec2 position, float radius){
        this.x = position.x;
        this.y = position.y;
        this.radius = radius;
        return this;
    }

    /**
     * Sets a new location and radius for this circle, based upon another circle.
     * @param circle The circle to copy the position and radius of.
     */
    public Circle set(Circle circle){
        this.x = circle.x;
        this.y = circle.y;
        this.radius = circle.radius;
        return this;
    }

    /**
     * Sets this {@link Circle}'s values in terms of its center and a point on its edge.
     * @param center The new center of the circle
     * @param edge Any point on the edge of the given circle
     */
    public Circle set(Vec2 center, Vec2 edge){
        this.x = center.x;
        this.y = center.y;
        this.radius = Mathf.len(center.x - edge.x, center.y - edge.y);
        return this;
    }

    /**
     * Sets the x and y-coordinates of circle center from vector
     * @param position The position vector
     */
    public Circle setPosition(Vec2 position){
        this.x = position.x;
        this.y = position.y;
        return this;
    }

    /**
     * Sets the x and y-coordinates of circle center
     * @param x The x-coordinate
     * @param y The y-coordinate
     */
    public Circle setPosition(float x, float y){
        this.x = x;
        this.y = y;
        return this;
    }

    /**
     * Sets the x-coordinate of circle center
     * @param x The x-coordinate
     */
    public void setX(float x){
        this.x = x;
    }

    /**
     * Sets the y-coordinate of circle center
     * @param y The y-coordinate
     */
    public void setY(float y){
        this.y = y;
    }

    /**
     * Sets the radius of circle
     * @param radius The radius
     */
    public void setRadius(float radius){
        this.radius = radius;
    }

    /**
     * Checks whether or not this circle contains a given point.
     * @param x X coordinate
     * @param y Y coordinate
     * @return true if this circle contains the given point.
     */
    public boolean contains(float x, float y){
        x = this.x - x;
        y = this.y - y;
        return x * x + y * y <= radius * radius;
    }

    /**
     * Checks whether or not this circle contains a given point.
     * @param point The {@link Vec2} that contains the point coordinates.
     * @return true if this circle contains this point; false otherwise.
     */
    public boolean contains(Vec2 point){
        float dx = x - point.x;
        float dy = y - point.y;
        return dx * dx + dy * dy <= radius * radius;
    }

    /**
     * @param c the other {@link Circle}
     * @return whether this circle contains the other circle.
     */
    public boolean contains(Circle c){
        final float radiusDiff = radius - c.radius;
        if(radiusDiff < 0f) return false; // Can't contain bigger circle
        final float dx = x - c.x;
        final float dy = y - c.y;
        final float dst = dx * dx + dy * dy;
        final float radiusSum = radius + c.radius;
        return (!(radiusDiff * radiusDiff < dst) && (dst < radiusSum * radiusSum));
    }

    /**
     * @param c the other {@link Circle}
     * @return whether this circle overlaps the other circle.
     */
    public boolean overlaps(Circle c){
        float dx = x - c.x;
        float dy = y - c.y;
        float distance = dx * dx + dy * dy;
        float radiusSum = radius + c.radius;
        return distance < radiusSum * radiusSum;
    }

    /** Returns a {@link String} representation of this {@link Circle} of the form {@code x,y,radius}. */
    @Override
    public String toString(){
        return x + "," + y + "," + radius;
    }

    /** @return The circumference of this circle (as 2 * {@link Mathf#PI2}) * {@code radius} */
    public float circumference(){
        return this.radius * Mathf.PI2;
    }

    /** @return The area of this circle (as {@link Mathf#PI} * radius * radius). */
    public float area(){
        return this.radius * this.radius * Mathf.PI;
    }

    @Override
    public boolean equals(Object o){
        if(o == this) return true;
        if(o == null || o.getClass() != this.getClass()) return false;
        Circle c = (Circle)o;
        return this.x == c.x && this.y == c.y && this.radius == c.radius;
    }

    @Override
    public int hashCode(){
        final int prime = 41;
        int result = 1;
        result = prime * result + Float.floatToRawIntBits(radius);
        result = prime * result + Float.floatToRawIntBits(x);
        result = prime * result + Float.floatToRawIntBits(y);
        return result;
    }
}
