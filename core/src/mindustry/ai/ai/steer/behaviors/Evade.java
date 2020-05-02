package mindustry.ai.ai.steer.behaviors;

import mindustry.ai.ai.steer.*;

/**
 * {@code Evade} behavior is almost the same as {@link Pursue} except that the agent flees from the estimated future position of
 * the pursuer. Indeed, reversing the acceleration is all we have to do.
 * @author davebaol
 */
public class Evade extends Pursue{

    /**
     * Creates a {@code Evade} behavior for the specified owner and target. Maximum prediction time defaults to 1 second.
     * @param owner the owner of this behavior
     * @param target the target of this behavior, typically a pursuer.
     */
    public Evade(Steerable owner, Steerable target){
        this(owner, target, 1);
    }

    /**
     * Creates a {@code Evade} behavior for the specified owner and pursuer.
     * @param owner the owner of this behavior
     * @param target the target of this behavior, typically a pursuer
     * @param maxPredictionTime the max time used to predict the pursuer's position assuming it continues to move with its current
     * velocity.
     */
    public Evade(Steerable owner, Steerable target, float maxPredictionTime){
        super(owner, target, maxPredictionTime);
    }

    @Override
    protected float getActualMaxLinearAcceleration(){
        // Simply return the opposite of the max linear acceleration so to evade the target
        return -getActualLimiter().getMaxLinearAcceleration();
    }

}
