package mindustry.ai.ai.steer.behaviors;

import arc.math.geom.*;
import mindustry.ai.ai.steer.*;
import mindustry.ai.ai.steer.Proximity.*;
import mindustry.ai.ai.steer.proximities.*;
import mindustry.ai.ai.utils.*;

/**
 * This behavior attempts to position a owner so that an obstacle is always between itself and the agent (the hunter) it's trying
 * to hide from. First the distance to each of these obstacles is determined. Then the owner uses the arrive behavior to steer
 * toward the closest one. If no appropriate obstacles can be found, no steering is returned.
 * <p>
 * You can use this behavior not only for situations where you require a non-player character (NPC) to hide from the player, like
 * find cover when fired at, but also in situations where you would like an NPC to sneak up on a player. For example, you can
 * create an NPC capable of stalking a player through a gloomy forest, darting from tree to tree.
 * <p>
 * It's worth mentioning that since this behavior can produce no steering acceleration it is commonly used with
 * {@link PrioritySteering}. For instance, to make the owner go away from the target if there are no obstacles nearby to hide
 * behind, just use {@link Hide} and {@link Evade} behaviors with this priority order.
 * <p>
 * There are a few interesting modifications you might want to make to this behavior:
 * <ul>
 * <li>With {@link FieldOfViewProximity} you can allow the owner to hide only if the target is within its field of view. This
 * tends to produce unsatisfactory performance though, because the owner starts to act like a child hiding from monsters beneath
 * the bed sheets, something like "if you can't see it, then it can't see you" effect making the owner look dumb. This can be
 * countered slightly though by adding in a time effect so that the owner will hide if the target is visible or if it has seen the
 * target within the last {@code N} seconds. This gives it a sort of memory and produces reasonable-looking behavior.</li>
 * <li>The same as above, but this time the owner only tries to hide if the owner can see the target and the target can see the
 * owner.
 * <li>It might be desirable to produce a force that steers the owner so that it always favors hiding positions that are to the
 * side or rear of the pursuer. This can be achieved easily using the dot product to bias the distances returned from the method
 * {@link #getHidingPosition}.</li>
 * <li>At the beginning of any of the methods a check can be made to test if the target is within a "threat distance" before
 * proceeding with any further calculations. If the target is not a threat, then the method can return immediately with zero
 * steering.</li>
 * </ul>
 * @author davebaol
 */
public class Hide extends Arrive implements ProximityCallback{
    /** The proximity to find nearby obstacles. */
    public Proximity proximity;
    /** The distance from the boundary of the obstacle behind which to hide. */
    public float distanceFromBoundary;

    private Vec2 toObstacle;
    private Vec2 bestHidingSpot;
    private float distance2ToClosest;

    /**
     * Creates an {@code Hide} behavior for the specified owner.
     * @param owner the owner of this behavior
     */
    public Hide(Steerable owner){
        this(owner, null);
    }

    /**
     * Creates a {@code Hide} behavior for the specified owner and target.
     * @param owner the owner of this behavior
     * @param target the target of this behavior
     */
    public Hide(Steerable owner, Location target){
        this(owner, target, null);
    }

    /**
     * Creates a {@code Hide} behavior for the specified owner, target and proximity.
     * @param owner the owner of this behavior
     * @param target the target of this behavior
     * @param proximity the proximity to find nearby obstacles
     */
    public Hide(Steerable owner, Location target, Proximity proximity){
        super(owner, target);
        this.proximity = proximity;

        this.bestHidingSpot = newVector(owner);
        this.toObstacle = null; // Set to null since we'll reuse steering.linear for this vector
    }

    @Override
    protected SteeringAcceleration calculateRealSteering(SteeringAcceleration steering){
        // Initialize member variables used by the callback
        this.distance2ToClosest = Float.POSITIVE_INFINITY;
        this.toObstacle = steering.linear;

        // Find neighbors (the obstacles) using this behavior as callback
        int neighborsCount = proximity.findNeighbors(this);

        // If no suitable obstacles found return no steering otherwise use Arrive on the hiding spot
        return neighborsCount == 0 ? steering.setZero() : arrive(steering, bestHidingSpot);
    }

    @Override
    public boolean report(Steerable neighbor){
        // Calculate the position of the hiding spot for this obstacle
        Vec2 hidingSpot = getHidingPosition(neighbor.getPosition(), neighbor.getBoundingRadius(), target.getPosition());

        // Work in distance-squared space to find the closest hiding
        // spot to the owner
        float distance2 = hidingSpot.dst2(owner.getPosition());
        if(distance2 < distance2ToClosest){
            distance2ToClosest = distance2;
            bestHidingSpot.set(hidingSpot);
            return true;
        }

        return false;
    }

    /**
     * Given the position of a target and the position and radius of an obstacle, this method calculates a position
     * {@code distanceFromBoundary} away from the object's bounding radius and directly opposite the target. It does this by scaling
     * the normalized "to obstacle" vector by the required distance away from the center of the obstacle and then adding the result
     * to the obstacle's position.
     * @return the hiding position behind the obstacle.
     */
    protected Vec2 getHidingPosition(Vec2 obstaclePosition, float obstacleRadius, Vec2 targetPosition){
        // Calculate how far away the agent is to be from the chosen
        // obstacle's bounding radius
        float distanceAway = obstacleRadius + distanceFromBoundary;

        // Calculate the normalized vector toward the obstacle from the target
        toObstacle.set(obstaclePosition).sub(targetPosition).nor();

        // Scale it to size and add to the obstacle's position to get
        // the hiding spot.
        return toObstacle.scl(distanceAway).add(obstaclePosition);
    }

}
