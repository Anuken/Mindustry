package mindustry.ai.ai.steer.behaviors;

import arc.math.*;
import mindustry.ai.ai.steer.*;
import mindustry.ai.ai.utils.*;

/**
 * {@code ReachOrientation} tries to align the owner to the target. It pays no attention to the position or velocity of the owner
 * or target. This steering behavior does not produce any linear acceleration; it only responds by turning.
 * <p>
 * {@code ReachOrientation} behaves in a similar way to {@link Arrive} since it tries to reach the target orientation and tries to
 * have zero rotation when it gets there. Like arrive, it uses two radii: {@code decelerationRadius} for slowing down and
 * {@code alignTolerance} to make orientations near the target acceptable without letting small errors keep the owner swinging.
 * Because we are dealing with a single scalar value, rather than a 2D or 3D vector, the radius acts as an interval.
 * <p>
 * Similarly to {@code Arrive}, there is a {@code timeToTarget} that defaults to 0.1 seconds.
 * @author davebaol
 */
public class ReachOrientation extends SteeringBehavior{

    /** The target to align to. */
    protected Location target;

    /** The tolerance for aligning to the target without letting small errors keep the owner swinging. */
    protected float alignTolerance;

    /** The radius for beginning to slow down */
    protected float decelerationRadius;

    /** The time over which to achieve target rotation speed */
    protected float timeToTarget = 0.1f;

    /**
     * Creates a {@code ReachOrientation} behavior for the specified owner.
     * @param owner the owner of this behavior.
     */
    public ReachOrientation(Steerable owner){
        this(owner, null);
    }

    /**
     * Creates a {@code ReachOrientation} behavior for the specified owner and target.
     * @param owner the owner of this behavior
     * @param target the target.
     */
    public ReachOrientation(Steerable owner, Location target){
        super(owner);
        this.target = target;
    }

    @Override
    protected SteeringAcceleration calculateRealSteering(SteeringAcceleration steering){
        return reachOrientation(steering, target.getOrientation());
    }

    /**
     * Produces a steering that tries to align the owner to the target orientation. This method is called by subclasses that want
     * to align to a certain orientation.
     * @param steering the steering to be calculated.
     * @param targetOrientation the target orientation you want to align to.
     * @return the calculated steering for chaining.
     */
    protected SteeringAcceleration reachOrientation(SteeringAcceleration steering, float targetOrientation){
        // Get the rotation direction to the target wrapped to the range [-PI, PI]
        float rotation = Mathf.wrapAngleAroundZero(targetOrientation - owner.getOrientation());

        // Absolute rotation
        float rotationSize = rotation < 0f ? -rotation : rotation;

        // Check if we are there, return no steering
        if(rotationSize <= alignTolerance) return steering.setZero();

        Limiter actualLimiter = getActualLimiter();

        // Use maximum rotation
        float targetRotation = actualLimiter.getMaxAngularSpeed();

        // If we are inside the slow down radius, then calculate a scaled rotation
        if(rotationSize <= decelerationRadius) targetRotation *= rotationSize / decelerationRadius;

        // The final target rotation combines
        // speed (already in the variable) and direction
        targetRotation *= rotation / rotationSize;

        // Acceleration tries to get to the target rotation
        steering.angular = (targetRotation - owner.getAngularVelocity()) / timeToTarget;

        // Check if the absolute acceleration is too great
        float angularAcceleration = steering.angular < 0f ? -steering.angular : steering.angular;
        if(angularAcceleration > actualLimiter.getMaxAngularAcceleration())
            steering.angular *= actualLimiter.getMaxAngularAcceleration() / angularAcceleration;

        // No linear acceleration
        steering.linear.setZero();

        // Output the steering
        return steering;
    }

    /** Returns the target to align to. */
    public Location getTarget(){
        return target;
    }

    /**
     * Sets the target to align to.
     * @return this behavior for chaining.
     */
    public ReachOrientation setTarget(Location target){
        this.target = target;
        return this;
    }

    /** Returns the tolerance for aligning to the target without letting small errors keep the owner swinging. */
    public float getAlignTolerance(){
        return alignTolerance;
    }

    /**
     * Sets the tolerance for aligning to the target without letting small errors keep the owner swinging.
     * @return this behavior for chaining.
     */
    public ReachOrientation setAlignTolerance(float alignTolerance){
        this.alignTolerance = alignTolerance;
        return this;
    }

    /** Returns the radius for beginning to slow down */
    public float getDecelerationRadius(){
        return decelerationRadius;
    }

    /**
     * Sets the radius for beginning to slow down
     * @return this behavior for chaining.
     */
    public ReachOrientation setDecelerationRadius(float decelerationRadius){
        this.decelerationRadius = decelerationRadius;
        return this;
    }

    /** Returns the time over which to achieve target rotation speed */
    public float getTimeToTarget(){
        return timeToTarget;
    }

    /**
     * Sets the time over which to achieve target rotation speed
     * @return this behavior for chaining.
     */
    public ReachOrientation setTimeToTarget(float timeToTarget){
        this.timeToTarget = timeToTarget;
        return this;
    }

    //
    // Setters overridden in order to fix the correct return type for chaining
    //

    @Override
    public ReachOrientation setOwner(Steerable owner){
        this.owner = owner;
        return this;
    }

    @Override
    public ReachOrientation setEnabled(boolean enabled){
        this.enabled = enabled;
        return this;
    }

    /**
     * Sets the limiter of this steering behavior. The given limiter must at least take care of the maximum angular speed and
     * acceleration.
     * @return this behavior for chaining.
     */
    @Override
    public ReachOrientation setLimiter(Limiter limiter){
        this.limiter = limiter;
        return this;
    }

}
