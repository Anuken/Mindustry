package mindustry.ai.ai.steer;

import arc.math.geom.*;
import mindustry.ai.ai.utils.*;

/**
 * A {@code Steerable} is a {@link Location} that gives access to the character's data required by steering system.
 * <p>
 * Notice that there is nothing to connect the direction that a Steerable is moving and the direction it is facing. For
 * instance, a character can be oriented along the x-axis but be traveling directly along the y-axis.
 * @author davebaol
 */
public interface Steerable extends Location, Limiter{

    /** Returns the vector indicating the linear velocity of this Steerable. */
    Vec2 getLinearVelocity();

    /** Returns the float value indicating the the angular velocity in radians of this Steerable. */
    float getAngularVelocity();

    /** Returns the bounding radius of this Steerable. */
    float getBoundingRadius();

    /** Returns {@code true} if this Steerable is tagged; {@code false} otherwise. */
    boolean isTagged();

    /**
     * Tag/untag this Steerable. This is a generic flag utilized in a variety of ways.
     * @param tagged the boolean value to set
     */
    void setTagged(boolean tagged);

}
