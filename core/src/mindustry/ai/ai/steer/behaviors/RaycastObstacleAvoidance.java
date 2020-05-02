package mindustry.ai.ai.steer.behaviors;

import arc.math.geom.*;
import mindustry.ai.ai.steer.*;
import mindustry.ai.ai.steer.utils.*;
import mindustry.ai.ai.steer.utils.rays.*;
import mindustry.ai.ai.utils.Ray;
import mindustry.ai.ai.utils.*;

/**
 * With the {@code RaycastObstacleAvoidance} the moving agent (the owner) casts one or more rays out in the direction of its
 * motion. If these rays collide with an obstacle, then a target is created that will avoid the collision, and the owner does a
 * basic seek on this target. Typically, the rays extend a short distance ahead of the character (usually a distance corresponding
 * to a few seconds of movement).
 * <p>
 * This behavior is especially suitable for large-scale obstacles like walls.
 * <p>
 * You should use the {@link RayConfiguration} more suitable for your game environment. Some basic ray configurations are provided
 * by the framework: {@link SingleRayConfiguration}, {@link ParallelSideRayConfiguration} and
 * {@link CentralRayWithWhiskersConfiguration}. There are no hard and fast rules as to which configuration is better. Each has its
 * own particular idiosyncrasies. A single ray with short whiskers is often the best initial configuration to try but can make it
 * impossible for the character to move down tight passages. The single ray configuration is useful in concave environments but
 * grazes convex obstacles. The parallel configuration works well in areas where corners are highly obtuse but is very susceptible
 * to the corner trap.
 * <p>
 * <a name="cornerTrap">
 * <h2>The corner trap</h2></a> All the basic configurations for multi-ray obstacle avoidance can suffer from a crippling problem
 * with acute angled corners (any convex corner, in fact, but it is more prevalent with acute angles). Consider a character with
 * two parallel rays that is going towards a corner. As soon as its left ray is colliding with the wall near the corner, the
 * steering behavior will turn it to the left to avoid the collision. Immediately, the right ray will then be colliding the other
 * side of the corner, and the steering behavior will turn the character to the right. The character will repeatedly collide both
 * sides of the corner in rapid succession. It will appear to home into the corner directly, until it slams into the wall. It will
 * be unable to free itself from the trap.
 * <p>
 * The fan structure, with a wide enough fan angle, alleviates this problem. Often, there is a trade-off, however, between
 * avoiding the corner trap with a large fan angle and keeping the angle small to allow the character to access small passages. At
 * worst, with a fan angle near PI radians, the character will not be able to respond quickly enough to collisions detected on its
 * side rays and will still graze against walls. There are two approaches that work well and represent the most practical
 * solutions to the problem:
 * <ul>
 * <li><b>Adaptive fan angles:</b> If the character is moving successfully without a collision, then the fan angle is narrowed. If
 * a collision is detected, then the fan angle is widened. If the character detects many collisions on successive frames, then the
 * fan angle will continue to widen, reducing the chance that the character is trapped in a corner.</li>
 * <li><b>Winner ray:</b> If a corner trap is detected, then one of the rays is considered to have won, and the collisions
 * detected by other rays are ignored for a while.</li>
 * </ul>
 * It seems that the most practical solution is to use adaptive fan angles, with one long ray cast and two shorter whiskers.
 * @author davebaol
 */
public class RaycastObstacleAvoidance extends SteeringBehavior{

    /** The inputRay configuration */
    protected RayConfiguration rayConfiguration;

    /** The collision detector */
    protected RaycastCollisionDetector raycastCollisionDetector;

    /** The minimum distance to a wall, i.e. how far to avoid collision. */
    protected float distanceFromBoundary;

    private Collision outputCollision;
    private Collision minOutputCollision;

    /**
     * Creates a {@code RaycastObstacleAvoidance} behavior.
     * @param owner the owner of this behavior
     */
    public RaycastObstacleAvoidance(Steerable owner){
        this(owner, null);
    }

    /**
     * Creates a {@code RaycastObstacleAvoidance} behavior.
     * @param owner the owner of this behavior
     * @param rayConfiguration the ray configuration
     */
    public RaycastObstacleAvoidance(Steerable owner, RayConfiguration rayConfiguration){
        this(owner, rayConfiguration, null);
    }

    /**
     * Creates a {@code RaycastObstacleAvoidance} behavior.
     * @param owner the owner of this behavior
     * @param rayConfiguration the ray configuration
     * @param raycastCollisionDetector the collision detector
     */
    public RaycastObstacleAvoidance(Steerable owner, RayConfiguration rayConfiguration,
                                    RaycastCollisionDetector raycastCollisionDetector){
        this(owner, rayConfiguration, raycastCollisionDetector, 0);
    }

    /**
     * Creates a {@code RaycastObstacleAvoidance} behavior.
     * @param owner the owner of this behavior
     * @param rayConfiguration the ray configuration
     * @param raycastCollisionDetector the collision detector
     * @param distanceFromBoundary the minimum distance to a wall (i.e., how far to avoid collision).
     */
    public RaycastObstacleAvoidance(Steerable owner, RayConfiguration rayConfiguration,
                                    RaycastCollisionDetector raycastCollisionDetector, float distanceFromBoundary){
        super(owner);
        this.rayConfiguration = rayConfiguration;
        this.raycastCollisionDetector = raycastCollisionDetector;
        this.distanceFromBoundary = distanceFromBoundary;

        this.outputCollision = new Collision(newVector(owner), newVector(owner));
        this.minOutputCollision = new Collision(newVector(owner), newVector(owner));
    }

    @Override
    protected SteeringAcceleration calculateRealSteering(SteeringAcceleration steering){
        Vec2 ownerPosition = owner.getPosition();
        float minDistanceSquare = Float.POSITIVE_INFINITY;

        // Get the updated rays
        Ray[] inputRays = rayConfiguration.updateRays();

        // Process rays
        for(Ray inputRay : inputRays){
            // Find the collision with current ray
            boolean collided = raycastCollisionDetector.findCollision(outputCollision, inputRay);

            if(collided){
                float distanceSquare = ownerPosition.dst2(outputCollision.point);
                if(distanceSquare < minDistanceSquare){
                    minDistanceSquare = distanceSquare;
                    // Swap collisions
                    Collision tmpCollision = outputCollision;
                    outputCollision = minOutputCollision;
                    minOutputCollision = tmpCollision;
                }
            }
        }

        // Return zero steering if no collision has occurred
        if(minDistanceSquare == Float.POSITIVE_INFINITY) return steering.setZero();

        // Calculate and seek the target position
        steering.linear.set(minOutputCollision.point)
        .mulAdd(minOutputCollision.normal, owner.getBoundingRadius() + distanceFromBoundary).sub(owner.getPosition()).nor()
        .scl(getActualLimiter().getMaxLinearAcceleration());

        // No angular acceleration
        steering.angular = 0;

        // Output steering acceleration
        return steering;
    }

    /** Returns the ray configuration of this behavior. */
    public RayConfiguration getRayConfiguration(){
        return rayConfiguration;
    }

    /**
     * Sets the ray configuration of this behavior.
     * @param rayConfiguration the ray configuration to set
     * @return this behavior for chaining.
     */
    public RaycastObstacleAvoidance setRayConfiguration(RayConfiguration rayConfiguration){
        this.rayConfiguration = rayConfiguration;
        return this;
    }

    /** Returns the raycast collision detector of this behavior. */
    public RaycastCollisionDetector getRaycastCollisionDetector(){
        return raycastCollisionDetector;
    }

    /**
     * Sets the raycast collision detector of this behavior.
     * @param raycastCollisionDetector the raycast collision detector to set
     * @return this behavior for chaining.
     */
    public RaycastObstacleAvoidance setRaycastCollisionDetector(RaycastCollisionDetector raycastCollisionDetector){
        this.raycastCollisionDetector = raycastCollisionDetector;
        return this;
    }

    /** Returns the distance from boundary, i.e. the minimum distance to an obstacle. */
    public float getDistanceFromBoundary(){
        return distanceFromBoundary;
    }

    /**
     * Sets the distance from boundary, i.e. the minimum distance to an obstacle.
     * @param distanceFromBoundary the distanceFromBoundary to set
     * @return this behavior for chaining.
     */
    public RaycastObstacleAvoidance setDistanceFromBoundary(float distanceFromBoundary){
        this.distanceFromBoundary = distanceFromBoundary;
        return this;
    }

    //
    // Setters overridden in order to fix the correct return type for chaining
    //

    @Override
    public RaycastObstacleAvoidance setOwner(Steerable owner){
        this.owner = owner;
        return this;
    }

    @Override
    public RaycastObstacleAvoidance setEnabled(boolean enabled){
        this.enabled = enabled;
        return this;
    }

    /**
     * Sets the limiter of this steering behavior. The given limiter must at least take care of the maximum linear acceleration.
     * @return this behavior for chaining.
     */
    @Override
    public RaycastObstacleAvoidance setLimiter(Limiter limiter){
        this.limiter = limiter;
        return this;
    }

}
