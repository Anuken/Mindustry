package mindustry.ai.ai.steer.behaviors;

import arc.math.geom.*;
import mindustry.ai.ai.steer.*;
import mindustry.ai.ai.steer.Proximity.*;

/**
 * {@code CollisionAvoidance} behavior steers the owner to avoid obstacles lying in its path. An obstacle is any object that can be
 * approximated by a circle (or sphere, if you are working in 3D).
 * <p>
 * This implementation uses collision prediction working out the closest approach of two agents and determining if their distance
 * at this point is less than the sum of their bounding radius. For avoiding groups of characters, averaging positions and
 * velocities do not work well with this approach. Instead, the algorithm needs to search for the character whose closest approach
 * will occur first and to react to this character only. Once this imminent collision is avoided, the steering behavior can then
 * react to more distant characters.
 * <p>
 * This algorithm works well with small and/or moving obstacles whose shape can be approximately represented by a center and a
 * radius.
 * @author davebaol
 */
public class CollisionAvoidance extends GroupBehavior implements ProximityCallback{
    private float shortestTime;
    private Steerable firstNeighbor;
    private float firstMinSeparation;
    private float firstDistance;
    private Vec2 firstRelativePosition;
    private Vec2 firstRelativeVelocity;
    private Vec2 relativePosition;
    private Vec2 relativeVelocity;

    /**
     * Creates a {@code CollisionAvoidance} behavior for the specified owner and proximity.
     * @param owner the owner of this behavior
     * @param proximity the proximity of this behavior.
     */
    public CollisionAvoidance(Steerable owner, Proximity proximity){
        super(owner, proximity);

        this.firstRelativePosition = newVector(owner);
        this.firstRelativeVelocity = newVector(owner);

        this.relativeVelocity = newVector(owner);
    }

    @Override
    protected SteeringAcceleration calculateRealSteering(SteeringAcceleration steering){
        shortestTime = Float.POSITIVE_INFINITY;
        firstNeighbor = null;
        firstMinSeparation = 0;
        firstDistance = 0;
        relativePosition = steering.linear;

        // Take into consideration each neighbor to find the most imminent collision.
        int neighborCount = proximity.findNeighbors(this);

        // If we have no target, then return no steering acceleration
        //
        // NOTE: You might think that the condition below always evaluates to true since
        // firstNeighbor has been set to null when entering this method. In fact, we have just
        // executed findNeighbors(this) that has possibly set firstNeighbor to a non null value
        // through the method reportNeighbor defined below.
        if(neighborCount == 0 || firstNeighbor == null) return steering.setZero();

        // If we're going to hit exactly, or if we're already
        // colliding, then do the steering based on current position.
        if(firstMinSeparation <= 0 || firstDistance < owner.getBoundingRadius() + firstNeighbor.getBoundingRadius()){
            relativePosition.set(firstNeighbor.getPosition()).sub(owner.getPosition());
        }else{
            // Otherwise calculate the future relative position
            relativePosition.set(firstRelativePosition).mulAdd(firstRelativeVelocity, shortestTime);
        }

        // Avoid the target
        // Notice that steerling.linear and relativePosition are the same vector
        relativePosition.nor().scl(-getActualLimiter().getMaxLinearAcceleration());

        // No angular acceleration
        steering.angular = 0f;

        // Output the steering
        return steering;
    }

    @Override
    public boolean report(Steerable neighbor){
        // Calculate the time to collision
        relativePosition.set(neighbor.getPosition()).sub(owner.getPosition());
        relativeVelocity.set(neighbor.getLinearVelocity()).sub(owner.getLinearVelocity());
        float relativeSpeed2 = relativeVelocity.len2();

        // Collision can't happen when the agents have the same linear velocity.
        // Also, note that timeToCollision would be NaN due to the indeterminate form 0/0 and,
        // since any comparison involving NaN returns false, it would become the shortestTime,
        // so defeating the algorithm.
        if(relativeSpeed2 == 0) return false;

        float timeToCollision = -relativePosition.dot(relativeVelocity) / relativeSpeed2;

        // If timeToCollision is negative, i.e. the owner is already moving away from the the neighbor,
        // or it's not the most imminent collision then no action needs to be taken.
        if(timeToCollision <= 0 || timeToCollision >= shortestTime) return false;

        // Check if it is going to be a collision at all
        float distance = relativePosition.len();
        float minSeparation = distance - (float)Math.sqrt(relativeSpeed2) * timeToCollision /* shortestTime */;
        if(minSeparation > owner.getBoundingRadius() + neighbor.getBoundingRadius()) return false;

        // Store most imminent collision data
        shortestTime = timeToCollision;
        firstNeighbor = neighbor;
        firstMinSeparation = minSeparation;
        firstDistance = distance;
        firstRelativePosition.set(relativePosition);
        firstRelativeVelocity.set(relativeVelocity);

        return true;
    }

}
