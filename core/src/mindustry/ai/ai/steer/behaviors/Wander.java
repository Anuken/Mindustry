package mindustry.ai.ai.steer.behaviors;

import arc.math.*;
import arc.math.geom.*;
import mindustry.ai.ai.steer.*;
import mindustry.ai.ai.utils.*;

/**
 * {@code Wander} behavior is designed to produce a steering acceleration that will give the impression of a random walk through
 * the agent's environment. You'll often find it a useful ingredient when creating an agent's behavior.
 * <p>
 * There is a circle in front of the owner (where front is determined by its current facing direction) on which the target is
 * constrained. Each time the behavior is run, we move the target around the circle a little, by a random amount. Now there are 2
 * ways to implement wander behavior:
 * <ul>
 * <li>The owner seeks the target, using the {@link Seek} behavior, and performs a {@link LookWhereYouAreGoing} behavior to
 * correct its orientation.</li>
 * <li>The owner tries to face the target in each frame, using the {@link Face} behavior to align to the target, and applies full
 * linear acceleration in the direction of its current orientation.</li>
 * </ul>
 * In either case, the orientation of the owner is retained between calls (so smoothing the changes in orientation). The angles
 * that the edges of the circle subtend to the owner determine how fast it will turn. If the target is on one of these extreme
 * points, it will turn quickly. The target will twitch and jitter around the edge of the circle, but the owner's orientation will
 * change smoothly.
 * <p>
 * This implementation uses the second approach. However, if you manually align owner's orientation to its linear velocity on each
 * time step, {@link Face} behavior should not be used (which is the default case). On the other hand, if the owner has
 * independent facing you should explicitly call {@link #setFaceEnabled(boolean) setFaceEnabled(true)} before using Wander
 * behavior.
 * <p>
 * Note that this behavior internally calls the {@link Timepiece#getTime() GdxAI.getTimepiece().getTime()} method to get the
 * current AI time and make the {@link #wanderRate} FPS independent. This means that
 * <ul>
 * <li>if you forget to {@link Timepiece#update(float) update the timepiece} the wander orientation won't change.</li>
 * <li>ideally the timepiece should be always updated before this steering behavior runs.</li>
 * </ul>
 * <p>
 * This steering behavior can be used to produce a whole range of random motion, from very smooth undulating turns to wild
 * Strictly Ballroom type whirls and pirouettes depending on the size of the circle, its distance from the agent, and the amount
 * of random displacement each frame.
 * @author davebaol
 */
public class Wander extends Face{
    /** The forward offset of the wander circle */
    public float wanderOffset;
    /** The radius of the wander circle */
    public float wanderRadius;
    /** The rate, expressed in radian per second, at which the wander orientation can change */
    public float wanderRate;
    /** The last time the orientation of the wander target has been updated */
    public float lastTime;
    /** The current orientation of the wander target */
    public float wanderOrientation;
    /**
     * The flag indicating whether to use {@link Face} behavior or not. This should be set to {@code true} when independent facing
     * is used.
     */
    public boolean faceEnabled;

    private Vec2 internalTargetPosition;
    private Vec2 wanderCenter;

    /**
     * Creates a {@code Wander} behavior for the specified owner.
     * @param owner the owner of this behavior.
     */
    public Wander(Steerable owner){
        super(owner);

        this.internalTargetPosition = newVector(owner);
        this.wanderCenter = newVector(owner);
    }

    @Override
    protected SteeringAcceleration calculateRealSteering(SteeringAcceleration steering){
        // Update the wander orientation
        float now = Timepiece.time;
        if(lastTime > 0){
            float delta = now - lastTime;
            wanderOrientation += Mathf.randomTriangular(wanderRate * delta);
        }
        lastTime = now;

        // Calculate the combined target orientation
        float targetOrientation = wanderOrientation + owner.getOrientation();

        // Calculate the center of the wander circle
        wanderCenter.set(owner.getPosition()).mulAdd(owner.angleToVector(steering.linear, owner.getOrientation()), wanderOffset);

        // Calculate the target location
        // Notice that we're using steering.linear as temporary vector
        internalTargetPosition.set(wanderCenter).mulAdd(owner.angleToVector(steering.linear, targetOrientation), wanderRadius);

        float maxLinearAcceleration = getActualLimiter().getMaxLinearAcceleration();

        if(faceEnabled){
            // Delegate to face
            face(steering, internalTargetPosition);

            // Set the linear acceleration to be at full
            // acceleration in the direction of the orientation
            owner.angleToVector(steering.linear, owner.getOrientation()).scl(maxLinearAcceleration);
        }else{
            // Seek the internal target position
            steering.linear.set(internalTargetPosition).sub(owner.getPosition()).nor().scl(maxLinearAcceleration);

            // No angular acceleration
            steering.angular = 0;

        }

        return steering;
    }
}
