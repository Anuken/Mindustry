package mindustry.ai.ai.utils;


import arc.math.*;
import arc.math.geom.*;

/**
 * The {@code Location} interface represents any game object having a position and an orientation.
 * @author davebaol
 */
public interface Location{

    /** Returns the vector indicating the position of this location. */
    Vec2 getPosition();

    /**
     * Returns the float value indicating the orientation of this location. The orientation is the angle in radians representing
     * the direction that this location is facing.
     */
    float getOrientation();

    /**
     * Sets the orientation of this location, i.e. the angle in radians representing the direction that this location is facing.
     * @param orientation the orientation in radians
     */
    void setOrientation(float orientation);

    /**
     * Returns the angle in radians pointing along the specified vector.
     * @param vector the vector
     */
    default float vectorToAngle(Vec2 vector){
        return Mathf.atan2(-vector.x, vector.y);
    }

    /**
     * Returns the unit vector in the direction of the specified angle expressed in radians.
     * @param outVector the output vector.
     * @param angle the angle in radians.
     * @return the output vector for chaining.
     */
    default Vec2 angleToVector(Vec2 outVector, float angle){
        return outVector.set(-Mathf.sin(angle), Mathf.cos(angle));
    }
}
