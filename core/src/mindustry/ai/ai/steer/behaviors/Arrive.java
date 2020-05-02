package mindustry.ai.ai.steer.behaviors;

import arc.math.geom.*;
import mindustry.ai.ai.steer.*;
import mindustry.ai.ai.utils.*;

/**
 * {@code Arrive} behavior moves the agent towards a target position. It is similar to seek but it attempts to arrive at the target
 * position with a zero velocity.
 * <p>
 * {@code Arrive} behavior uses two radii. The {@code arrivalTolerance} lets the owner get near enough to the target without
 * letting small errors keep it in motion. The {@code decelerationRadius}, usually much larger than the previous one, specifies
 * when the incoming character will begin to slow down. The algorithm calculates an ideal speed for the owner. At the slowing-down
 * radius, this is equal to its maximum linear speed. At the target point, it is zero (we want to have zero speed when we arrive).
 * In between, the desired speed is an interpolated intermediate value, controlled by the distance from the target.
 * <p>
 * The direction toward the target is calculated and combined with the desired speed to give a target velocity. The algorithm
 * looks at the current velocity of the character and works out the acceleration needed to turn it into the target velocity. We
 * can't immediately change velocity, however, so the acceleration is calculated based on reaching the target velocity in a fixed
 * time scale known as {@code timeToTarget}. This is usually a small value; it defaults to 0.1 seconds which is a good starting
 * point.
 * @author davebaol
 */
public class Arrive extends SteeringBehavior{
    /** The target to arrive to. */
    public Location target;
    /**
     * The tolerance for arriving at the target. It lets the owner get near enough to the target without letting small errors keep
     * it in motion.
     */
    public float arrivalTolerance;
    /** The radius for beginning to slow down */
    public float decelerationRadius;
    /** The time over which to achieve target speed */
    public float timeToTarget = 0.1f;

    /**
     * Creates an {@code Arrive} behavior for the specified owner.
     * @param owner the owner of this behavior
     */
    public Arrive(Steerable owner){
        this(owner, null);
    }

    /**
     * Creates an {@code Arrive} behavior for the specified owner and target.
     * @param owner the owner of this behavior
     * @param target the target of this behavior
     */
    public Arrive(Steerable owner, Location target){
        super(owner);
        this.target = target;
    }

    @Override
    protected SteeringAcceleration calculateRealSteering(SteeringAcceleration steering){
        return arrive(steering, target.getPosition());
    }

    protected SteeringAcceleration arrive(SteeringAcceleration steering, Vec2 targetPosition){
        // Get the direction and distance to the target
        Vec2 toTarget = steering.linear.set(targetPosition).sub(owner.getPosition());
        float distance = toTarget.len();

        // Check if we are there, return no steering
        if(distance <= arrivalTolerance) return steering.setZero();

        Limiter actualLimiter = getActualLimiter();
        // Go max speed
        float targetSpeed = actualLimiter.getMaxLinearSpeed();

        // If we are inside the slow down radius calculate a scaled speed
        if(distance <= decelerationRadius) targetSpeed *= distance / decelerationRadius;

        // Target velocity combines speed and direction
        Vec2 targetVelocity = toTarget.scl(targetSpeed / distance); // Optimized code for: toTarget.nor().scl(targetSpeed)

        // Acceleration tries to get to the target velocity without exceeding max acceleration
        // Notice that steering.linear and targetVelocity are the same vector
        targetVelocity.sub(owner.getLinearVelocity()).scl(1f / timeToTarget).limit(actualLimiter.getMaxLinearAcceleration());

        // No angular acceleration
        steering.angular = 0f;

        // Output the steering
        return steering;
    }
}
