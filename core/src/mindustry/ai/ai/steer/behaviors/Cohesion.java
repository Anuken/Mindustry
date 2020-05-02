package mindustry.ai.ai.steer.behaviors;

import arc.math.geom.*;
import mindustry.ai.ai.steer.*;
import mindustry.ai.ai.steer.Proximity.*;

/**
 * {@code Cohesion} is a group behavior producing a linear acceleration that attempts to move the agent towards the center of mass
 * of the agents in its immediate area defined by the given {@link Proximity}. The acceleration is calculated by first iterating
 * through all the neighbors and averaging their position vectors. This gives us the center of mass of the neighbors, the place
 * the agents wants to get to, so it seeks to that position.
 * <p>
 * A sheep running after its flock is demonstrating cohesive behavior. Use this behavior to keep a group of agents together.
 * @author davebaol
 */
public class Cohesion extends GroupBehavior implements ProximityCallback{

    private Vec2 centerOfMass;

    /**
     * Creates a {@code Cohesion} for the specified owner and proximity.
     * @param owner the owner of this behavior.
     * @param proximity the proximity to detect the owner's neighbors
     */
    public Cohesion(Steerable owner, Proximity proximity){
        super(owner, proximity);
    }

    @Override
    protected SteeringAcceleration calculateRealSteering(SteeringAcceleration steering){

        steering.setZero();

        centerOfMass = steering.linear;

        int neighborCount = proximity.findNeighbors(this);

        if(neighborCount > 0){

            // The center of mass is the average of the sum of positions
            centerOfMass.scl(1f / neighborCount);

            // Now seek towards that position.
            centerOfMass.sub(owner.getPosition()).nor().scl(getActualLimiter().getMaxLinearAcceleration());
        }

        return steering;
    }

    @Override
    public boolean report(Steerable neighbor){
        // Accumulate neighbor position
        centerOfMass.add(neighbor.getPosition());
        return true;
    }
}
