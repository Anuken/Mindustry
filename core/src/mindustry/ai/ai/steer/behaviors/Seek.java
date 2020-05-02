package mindustry.ai.ai.steer.behaviors;

import mindustry.ai.ai.steer.*;
import mindustry.ai.ai.utils.*;

/**
 * {@code Seek} behavior moves the owner towards the target position. Given a target, this behavior calculates the linear steering
 * acceleration which will direct the agent towards the target as fast as possible.
 * @author davebaol
 */
public class Seek extends SteeringBehavior{

    /** The target to seek */
    protected Location target;

    /**
     * Creates a {@code Seek} behavior for the specified owner.
     * @param owner the owner of this behavior.
     */
    public Seek(Steerable owner){
        this(owner, null);
    }

    /**
     * Creates a {@code Seek} behavior for the specified owner and target.
     * @param owner the owner of this behavior
     * @param target the target agent of this behavior.
     */
    public Seek(Steerable owner, Location target){
        super(owner);
        this.target = target;
    }

    @Override
    protected SteeringAcceleration calculateRealSteering(SteeringAcceleration steering){
        // Try to match the position of the character with the position of the target by calculating
        // the direction to the target and by moving toward it as fast as possible.
        steering.linear.set(target.getPosition()).sub(owner.getPosition()).nor().scl(getActualLimiter().getMaxLinearAcceleration());

        // No angular acceleration
        steering.angular = 0;

        // Output steering acceleration
        return steering;
    }

    /** Returns the target to seek. */
    public Location getTarget(){
        return target;
    }

    /**
     * Sets the target to seek.
     * @return this behavior for chaining.
     */
    public Seek setTarget(Location target){
        this.target = target;
        return this;
    }

    //
    // Setters overridden in order to fix the correct return type for chaining
    //

    @Override
    public Seek setOwner(Steerable owner){
        this.owner = owner;
        return this;
    }

    @Override
    public Seek setEnabled(boolean enabled){
        this.enabled = enabled;
        return this;
    }

    /**
     * Sets the limiter of this steering behavior. The given limiter must at least take care of the maximum linear acceleration.
     * @return this behavior for chaining.
     */
    @Override
    public Seek setLimiter(Limiter limiter){
        this.limiter = limiter;
        return this;
    }

}
