package mindustry.ai.ai.steer.behaviors;

import mindustry.ai.ai.steer.*;
import mindustry.ai.ai.utils.*;

/**
 * The entire steering framework assumes that the direction a character is facing does not have to be its direction of motion. In
 * many cases, however, you would like the character to face in the direction it is moving. To do this you can manually align the
 * orientation of the character to its linear velocity on each frame update or you can use the {@code LookWhereYouAreGoing}
 * behavior.
 * <p>
 * {@code LookWhereYouAreGoing} behavior gives the owner angular acceleration to make it face in the direction it is moving. In
 * this way the owner changes facing gradually, which can look more natural, especially for aerial vehicles such as helicopters or
 * for human characters that can move sideways.
 * <p>
 * This is a process similar to the {@code Face} behavior. The target orientation is calculated using the current velocity of the
 * owner. If there is no velocity, then the target orientation is set to the current orientation. We have no preference in this
 * situation for any orientation.
 * @author davebaol
 */
public class LookWhereYouAreGoing extends ReachOrientation{

    /**
     * Creates a {@code LookWhereYouAreGoing} behavior for the specified owner.
     * @param owner the owner of this behavior.
     */
    public LookWhereYouAreGoing(Steerable owner){
        super(owner);
    }

    @Override
    protected SteeringAcceleration calculateRealSteering(SteeringAcceleration steering){
        // Check for a zero direction, and return no steering if so
        if(owner.getLinearVelocity().isZero(getActualLimiter().getZeroLinearSpeedThreshold())) return steering.setZero();

        // Calculate the orientation based on the velocity of the owner
        float orientation = owner.vectorToAngle(owner.getLinearVelocity());

        // Delegate to ReachOrientation
        return reachOrientation(steering, orientation);
    }

    //
    // Setters overridden in order to fix the correct return type for chaining
    //

    @Override
    public LookWhereYouAreGoing setOwner(Steerable owner){
        this.owner = owner;
        return this;
    }

    @Override
    public LookWhereYouAreGoing setEnabled(boolean enabled){
        this.enabled = enabled;
        return this;
    }

    /**
     * Sets the limiter of this steering behavior. The given limiter must at least take care of the maximum angular speed and
     * acceleration.
     * @return this behavior for chaining.
     */
    @Override
    public LookWhereYouAreGoing setLimiter(Limiter limiter){
        this.limiter = limiter;
        return this;
    }

    /**
     * Sets the target to align to. Notice that this method is inherited from {@link ReachOrientation}, but is completely useless
     * for {@code LookWhereYouAreGoing} because the target orientation is determined by the velocity of the owner itself.
     * @return this behavior for chaining.
     */
    @Override
    public LookWhereYouAreGoing setTarget(Location target){
        this.target = target;
        return this;
    }

    @Override
    public LookWhereYouAreGoing setAlignTolerance(float alignTolerance){
        this.alignTolerance = alignTolerance;
        return this;
    }

    @Override
    public LookWhereYouAreGoing setDecelerationRadius(float decelerationRadius){
        this.decelerationRadius = decelerationRadius;
        return this;
    }

    @Override
    public LookWhereYouAreGoing setTimeToTarget(float timeToTarget){
        this.timeToTarget = timeToTarget;
        return this;
    }

}
