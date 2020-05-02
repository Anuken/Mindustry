package mindustry.ai.ai.steer.behaviors;

import arc.math.geom.*;
import mindustry.ai.ai.steer.*;
import mindustry.ai.ai.utils.*;

/**
 * {@code Face} behavior makes the owner look at its target. It delegates to the {@link ReachOrientation} behavior to perform the
 * rotation but calculates the target orientation first based on target and owner position.
 * @author davebaol
 */
public class Face extends ReachOrientation{

    /**
     * Creates a {@code Face} behavior for the specified owner.
     * @param owner the owner of this behavior.
     */
    public Face(Steerable owner){
        this(owner, null);
    }

    /**
     * Creates a {@code Face} behavior for the specified owner and target.
     * @param owner the owner of this behavior
     * @param target the target of this behavior.
     */
    public Face(Steerable owner, Location target){
        super(owner, target);
    }

    @Override
    protected SteeringAcceleration calculateRealSteering(SteeringAcceleration steering){
        return face(steering, target.getPosition());
    }

    protected SteeringAcceleration face(SteeringAcceleration steering, Vec2 targetPosition){
        // Get the direction to target
        Vec2 toTarget = steering.linear.set(targetPosition).sub(owner.getPosition());

        // Check for a zero direction, and return no steering if so
        if(toTarget.isZero(getActualLimiter().getZeroLinearSpeedThreshold())) return steering.setZero();

        // Calculate the orientation to face the target
        float orientation = owner.vectorToAngle(toTarget);

        // Delegate to ReachOrientation
        return reachOrientation(steering, orientation);
    }

    //
    // Setters overridden in order to fix the correct return type for chaining
    //

    @Override
    public Face setOwner(Steerable owner){
        this.owner = owner;
        return this;
    }

    @Override
    public Face setEnabled(boolean enabled){
        this.enabled = enabled;
        return this;
    }

    /**
     * Sets the limiter of this steering behavior. The given limiter must at least take care of the maximum angular speed and
     * acceleration.
     * @return this behavior for chaining.
     */
    @Override
    public Face setLimiter(Limiter limiter){
        this.limiter = limiter;
        return this;
    }

    @Override
    public Face setTarget(Location target){
        this.target = target;
        return this;
    }

    @Override
    public Face setAlignTolerance(float alignTolerance){
        this.alignTolerance = alignTolerance;
        return this;
    }

    @Override
    public Face setDecelerationRadius(float decelerationRadius){
        this.decelerationRadius = decelerationRadius;
        return this;
    }

    @Override
    public Face setTimeToTarget(float timeToTarget){
        this.timeToTarget = timeToTarget;
        return this;
    }

}
