package mindustry.ai.ai.steer.behaviors;

import arc.math.geom.*;
import mindustry.ai.ai.steer.*;
import mindustry.ai.ai.steer.Proximity.*;

/**
 * {@code Separation} is a group behavior producing a steering acceleration repelling from the other neighbors which are the agents
 * in the immediate area defined by the given {@link Proximity}. The acceleration is calculated by iterating through all the
 * neighbors, examining each one. The vector to each agent under consideration is normalized, multiplied by a strength decreasing
 * according to the inverse square law in relation to distance, and accumulated.
 * @author davebaol
 */
public class Separation extends GroupBehavior implements ProximityCallback{
    /**
     * The constant coefficient of decay for the inverse square law force. It controls how fast the separation strength decays with
     * distance.
     */
    public float decayCoefficient = 1f;

    private Vec2 toAgent;
    private Vec2 linear;

    /**
     * Creates a {@code Separation} behavior for the specified owner and proximity.
     * @param owner the owner of this behavior
     * @param proximity the proximity to detect the owner's neighbors
     */
    public Separation(Steerable owner, Proximity proximity){
        super(owner, proximity);

        this.toAgent = newVector(owner);
    }

    @Override
    protected SteeringAcceleration calculateRealSteering(SteeringAcceleration steering){
        steering.setZero();

        linear = steering.linear;

        proximity.findNeighbors(this);

        return steering;
    }

    @Override
    public boolean report(Steerable neighbor){

        toAgent.set(owner.getPosition()).sub(neighbor.getPosition());
        float distanceSqr = toAgent.len2();

        if(distanceSqr == 0) return true;

        float maxAcceleration = getActualLimiter().getMaxLinearAcceleration();

        // Calculate the strength of repulsion through inverse square law decay
        float strength = decayCoefficient / distanceSqr;
        if(strength > maxAcceleration) strength = maxAcceleration;

        // Add the acceleration
        // Optimized code for linear.mulAdd(toAgent.nor(), strength);
        linear.mulAdd(toAgent, strength / (float)Math.sqrt(distanceSqr));

        return true;
    }
}
