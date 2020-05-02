package mindustry.ai.ai.steer.behaviors;

import arc.math.geom.*;
import mindustry.ai.ai.steer.*;

/**
 * {@code Interpose} behavior produces a steering force that moves the owner to a point along the imaginary line connecting two
 * other agents. A bodyguard taking a bullet for his employer or a soccer player intercepting a pass are examples of this type of
 * behavior. Like {@code Pursue}, the owner must estimate where the two agents are going to be located at a time {@code t} in the
 * future. It can then steer toward that position using the {@link Arrive} behavior. But how do we know what the best value of
 * {@code t} is to use? The answer is, we don't, so we make a calculated guess instead.
 * <p>
 * The first step is to determine a point along the imaginary line connecting the positions of the agents at the current time
 * step. This point is found taking into account the {@code interpositionRatio}, a number between 0 and 1 where 0 is the position
 * of the first agent (agentA) and 1 is the position of the second agent (agentB). Values in between are interpolated intermediate
 * locations.
 * <p>
 * Then the distance from this point is computed and the value divided by the owner's maximum speed to give the time {@code t}
 * required to travel the distance.
 * <p>
 * Using the time {@code t}, the agents' positions are extrapolated into the future. The target position in between of these
 * predicted positions is determined and finally the owner uses the Arrive behavior to steer toward that point.
 * @author davebaol
 */
public class Interpose extends Arrive{
    public Steerable agentA;
    public Steerable agentB;
    public float interpositionRatio;

    private Vec2 internalTargetPosition;

    /**
     * Creates an {@code Interpose} behavior for the specified owner and agents using the midpoint between agents as the target.
     * @param owner the owner of this behavior
     * @param agentA the first agent
     * @param agentB the other agent
     */
    public Interpose(Steerable owner, Steerable agentA, Steerable agentB){
        this(owner, agentA, agentB, 0.5f);
    }

    /**
     * Creates an {@code Interpose} behavior for the specified owner and agents using the the given interposing ratio.
     * @param owner the owner of this behavior
     * @param agentA the first agent
     * @param agentB the other agent
     * @param interpositionRatio a number between 0 and 1 indicating the percentage of the distance between the 2 agents that the
     * owner should reach, where 0 is agentA position and 1 is agentB position.
     */
    public Interpose(Steerable owner, Steerable agentA, Steerable agentB, float interpositionRatio){
        super(owner);
        this.agentA = agentA;
        this.agentB = agentB;
        this.interpositionRatio = interpositionRatio;

        this.internalTargetPosition = newVector(owner);
    }

    @Override
    protected SteeringAcceleration calculateRealSteering(SteeringAcceleration steering){
        // First we need to figure out where the two agents are going to be at
        // time Vec2 in the future. This is approximated by determining the time
        // taken by the owner to reach the desired point between the 2 agents
        // at the current time at the max speed. This desired point P is given by
        // P = posA + interpositionRatio * (posB - posA)
        internalTargetPosition.set(agentB.getPosition()).sub(agentA.getPosition()).scl(interpositionRatio)
        .add(agentA.getPosition());

        float timeToTargetPosition = owner.getPosition().dst(internalTargetPosition) / getActualLimiter().getMaxLinearSpeed();

        // Now we have the time, we assume that agent A and agent B will continue on a
        // straight trajectory and extrapolate to get their future positions.
        // Note that here we are reusing steering.linear vector as agentA future position
        // and targetPosition as agentB future position.
        steering.linear.set(agentA.getPosition()).mulAdd(agentA.getLinearVelocity(), timeToTargetPosition);
        internalTargetPosition.set(agentB.getPosition()).mulAdd(agentB.getLinearVelocity(), timeToTargetPosition);

        // Calculate the target position between these predicted positions
        internalTargetPosition.sub(steering.linear).scl(interpositionRatio).add(steering.linear);

        // Finally delegate to Arrive
        return arrive(steering, internalTargetPosition);
    }

    /** Returns the current position of the internal target. This method is useful for debug purpose. */
    public Vec2 getInternalTargetPosition(){
        return internalTargetPosition;
    }
}
