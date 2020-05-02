package mindustry.ai.ai.steer.behaviors;

import arc.math.geom.*;
import mindustry.ai.ai.steer.*;
import mindustry.ai.ai.utils.*;

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

    protected Steerable agentA;
    protected Steerable agentB;
    protected float interpositionRatio;

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

    /** Returns the first agent. */
    public Steerable getAgentA(){
        return agentA;
    }

    /**
     * Sets the first agent.
     * @return this behavior for chaining.
     */
    public Interpose setAgentA(Steerable agentA){
        this.agentA = agentA;
        return this;
    }

    /** Returns the second agent. */
    public Steerable getAgentB(){
        return agentB;
    }

    /**
     * Sets the second agent.
     * @return this behavior for chaining.
     */
    public Interpose setAgentB(Steerable agentB){
        this.agentB = agentB;
        return this;
    }

    /** Returns the interposition ratio. */
    public float getInterpositionRatio(){
        return interpositionRatio;
    }

    /**
     * Sets the interposition ratio.
     * @param interpositionRatio a number between 0 and 1 indicating the percentage of the distance between the 2 agents that the
     * owner should reach. Especially, 0 is the position of agentA and 1 is the position of agentB.
     * @return this behavior for chaining.
     */
    public Interpose setInterpositionRatio(float interpositionRatio){
        this.interpositionRatio = interpositionRatio;
        return this;
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

    //
    // Setters overridden in order to fix the correct return type for chaining
    //

    @Override
    public Interpose setOwner(Steerable owner){
        this.owner = owner;
        return this;
    }

    @Override
    public Interpose setEnabled(boolean enabled){
        this.enabled = enabled;
        return this;
    }

    @Override
    public Interpose setLimiter(Limiter limiter){
        this.limiter = limiter;
        return this;
    }

    @Override
    public Interpose setTarget(Location target){
        this.target = target;
        return this;
    }

    @Override
    public Interpose setArrivalTolerance(float arrivalTolerance){
        this.arrivalTolerance = arrivalTolerance;
        return this;
    }

    @Override
    public Interpose setDecelerationRadius(float decelerationRadius){
        this.decelerationRadius = decelerationRadius;
        return this;
    }

    @Override
    public Interpose setTimeToTarget(float timeToTarget){
        this.timeToTarget = timeToTarget;
        return this;
    }

}
