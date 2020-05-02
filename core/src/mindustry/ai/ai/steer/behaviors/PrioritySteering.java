package mindustry.ai.ai.steer.behaviors;

import arc.struct.*;
import mindustry.ai.ai.steer.*;

/**
 * The {@code PrioritySteering} behavior iterates through the behaviors and returns the first non zero steering. It makes sense
 * since certain steering behaviors only request an acceleration in particular conditions. Unlike {@link Seek} or {@link Evade},
 * which always produce an acceleration, {@link RaycastObstacleAvoidance}, {@link CollisionAvoidance}, {@link Separation},
 * {@link Hide} and {@link Arrive} will suggest no acceleration in many cases. But when these behaviors do suggest an
 * acceleration, it is unwise to ignore it. An obstacle avoidance behavior, for example, should be honored immediately to avoid
 * the crash.
 * <p>
 * Typically the behaviors of a {@code PrioritySteering} are arranged in groups with regular blending weights, see
 * {@link BlendedSteering}. These groups are then placed in priority order to let the steering system consider each group in turn.
 * It blends the steering behaviors in the current group together. If the total result is very small (less than some small, but
 * adjustable, parameter), then it is ignored and the next group is considered. It is best not to check against zero directly,
 * because numerical instability in calculations can mean that a zero value is never reached for some steering behaviors. Using a
 * small constant value (conventionally called {@code epsilon}) avoids this problem. When a group is found with a result that isn't
 * small, its result is used to steer the agent.
 * <p>
 * For instance, a pursuing agent working in a team may have three priorities:
 * <ul>
 * <li>a collision avoidance group that contains behaviors for obstacle avoidance, wall avoidance, and avoiding other characters.</li>
 * <li>a separation behavior used to avoid getting too close to other members of the chasing pack.</li>
 * <li>a pursuit behavior to chase the target.</li>
 * </ul>
 * If the character is far from any interference, the collision avoidance group will return with no desired acceleration. The
 * separation behavior will then be considered but will also return with no action. Finally, the pursuit behavior will be
 * considered, and the acceleration needed to continue the chase will be used. If the current motion of the character is perfect
 * for the pursuit, this behavior may also return with no acceleration. In this case, there are no more behaviors to consider, so
 * the character will have no acceleration, just as if they'd been exclusively controlled by the pursuit behavior.
 * <p>
 * In a different scenario, if the character is about to crash into a wall, the first group will return an acceleration that will
 * help avoid the crash. The character will carry out this acceleration immediately, and the steering behaviors in the other
 * groups won't be considered.
 * <p>
 * Usually {@code PrioritySteering} gives you a good compromise between speed and accuracy.
 * @author davebaol
 */
public class PrioritySteering extends SteeringBehavior{
    /** The threshold of the steering acceleration magnitude below which a steering behavior is considered to have given no output. */
    public float epsilon;
    /**
     * The list of steering behaviors in priority order. The first item in the list is tried first, the subsequent entries are only
     * considered if the first one does not return a result.
     */
    public Array<SteeringBehavior> behaviors = new Array<>();
    /** The index of the behavior whose acceleration has been returned by the last evaluation of this priority steering. */
    public int selectedBehaviorIndex;

    /**
     * Creates a {@code PrioritySteering} behavior for the specified owner. The threshold is set to 0.001.
     * @param owner the owner of this behavior
     */
    public PrioritySteering(Steerable owner){
        this(owner, 0.001f);
    }

    /**
     * Creates a {@code PrioritySteering} behavior for the specified owner and threshold.
     * @param owner the owner of this behavior
     * @param epsilon the threshold of the steering acceleration magnitude below which a steering behavior is considered to have
     * given no output
     */
    public PrioritySteering(Steerable owner, float epsilon){
        super(owner);
        this.epsilon = epsilon;
    }

    /**
     * Adds the specified behavior to the priority list.
     * @param behavior the behavior to add
     * @return this behavior for chaining.
     */
    public PrioritySteering add(SteeringBehavior behavior){
        behaviors.add(behavior);
        return this;
    }

    @Override
    protected SteeringAcceleration calculateRealSteering(SteeringAcceleration steering){
        // We'll need epsilon squared later.
        float epsilonSquared = epsilon * epsilon;

        // Go through the behaviors until one has a large enough acceleration
        int n = behaviors.size;
        selectedBehaviorIndex = -1;
        for(int i = 0; i < n; i++){
            selectedBehaviorIndex = i;

            SteeringBehavior behavior = behaviors.get(i);

            // Calculate the behavior's steering
            behavior.calculateSteering(steering);

            // If we're above the threshold return the current steering
            if(steering.calculateSquareMagnitude() > epsilonSquared) return steering;
        }

        // If we get here, it means that no behavior had a large enough acceleration,
        // so return the small acceleration from the final behavior or zero if there are
        // no behaviors in the list.
        return n > 0 ? steering : steering.setZero();
    }

}
