package mindustry.ai.ai.steer.behaviors;

import mindustry.ai.ai.steer.*;
import mindustry.ai.ai.utils.*;

/**
 * {@code Flee} behavior does the opposite of {@link Seek}. It produces a linear steering force that moves the agent away from a
 * target position.
 * @author davebaol
 */
public class Flee extends Seek{

    /**
     * Creates a {@code Flee} behavior for the specified owner.
     * @param owner the owner of this behavior.
     */
    public Flee(Steerable owner){
        this(owner, null);
    }

    /**
     * Creates a {@code Flee} behavior for the specified owner and target.
     * @param owner the owner of this behavior
     * @param target the target agent of this behavior.
     */
    public Flee(Steerable owner, Location target){
        super(owner, target);
    }

    @Override
    protected SteeringAcceleration calculateRealSteering(SteeringAcceleration steering){
        // We just do the opposite of seek, i.e. (owner.getPosition() - target.getPosition())
        // instead of (target.getPosition() - owner.getPosition())
        steering.linear.set(owner.getPosition()).sub(target.getPosition()).nor().scl(getActualLimiter().getMaxLinearAcceleration());

        // No angular acceleration
        steering.angular = 0;

        // Output steering acceleration
        return steering;
    }

    //
    // Setters overridden in order to fix the correct return type for chaining
    //

    @Override
    public Flee setOwner(Steerable owner){
        this.owner = owner;
        return this;
    }

    @Override
    public Flee setEnabled(boolean enabled){
        this.enabled = enabled;
        return this;
    }

    /**
     * Sets the limiter of this steering behavior. The given limiter must at least take care of the maximum linear acceleration.
     * @return this behavior for chaining.
     */
    @Override
    public Flee setLimiter(Limiter limiter){
        this.limiter = limiter;
        return this;
    }

    @Override
    public Flee setTarget(Location target){
        this.target = target;
        return this;
    }

}
