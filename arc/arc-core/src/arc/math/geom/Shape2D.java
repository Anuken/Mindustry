package arc.math.geom;

public interface Shape2D{

    /** Returns whether the given point is contained within the shape. */
    boolean contains(Vec2 point);

    /** Returns whether a point with the given coordinates is contained within the shape. */
    boolean contains(float x, float y);

}
