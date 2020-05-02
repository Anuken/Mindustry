package mindustry.ai.ai.steer.behaviors;

import arc.math.geom.*;
import mindustry.ai.ai.steer.*;
import mindustry.ai.ai.steer.Proximity.*;

/**
 * {@code Alignment} is a group behavior producing a linear acceleration that attempts to keep the owner aligned with the agents in
 * its immediate area defined by the given {@link Proximity}. The acceleration is calculated by first iterating through all the
 * neighbors and averaging their linear velocity vectors. This value is the desired direction, so we just subtract the owner's
 * linear velocity to get the steering output.
 * <p>
 * Cars moving along roads demonstrate {@code Alignment} type behavior. They also demonstrate {@link Separation} as they try to
 * keep a minimum distance from each other.
 * @author davebaol
 */
public class Alignment extends GroupBehavior implements ProximityCallback{
    private Vec2 averageVelocity;

    /**
     * Creates an {@code Alignment} behavior for the specified owner and proximity.
     * @param owner the owner of this behavior
     * @param proximity the proximity
     */
    public Alignment(Steerable owner, Proximity proximity){
        super(owner, proximity);
    }

    @Override
    protected SteeringAcceleration calculateRealSteering(SteeringAcceleration steering){
        steering.setZero();

        averageVelocity = steering.linear;

        int neighborCount = proximity.findNeighbors(this);

        if(neighborCount > 0){
            // Average the accumulated velocities
            averageVelocity.scl(1f / neighborCount);

            // Match the average velocity.
            // Notice that steering.linear and averageVelocity are the same vector here.
            averageVelocity.sub(owner.getLinearVelocity()).limit(getActualLimiter().getMaxLinearAcceleration());
        }

        return steering;
    }

    @Override
    public boolean report(Steerable neighbor){
        // Accumulate neighbor velocity
        averageVelocity.add(neighbor.getLinearVelocity());
        return true;
    }
}
