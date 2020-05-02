package mindustry.ai.ai.steer.behaviors;

import arc.struct.*;
import mindustry.ai.ai.steer.*;

/**
 * This combination behavior simply sums up all the behaviors, applies their weights, and truncates the result before returning.
 * There are no constraints on the blending weights; they don't have to sum to one, for example, and rarely do. Don't think of
 * {@code BlendedSteering} as a weighted mean, because it's not.
 * <p>
 * With {@code BlendedSteering} you can combine multiple behaviors to get a more complex behavior. It can work fine, but the
 * trade-off is that it comes with a few problems:
 * <ul>
 * <li>Since every active behavior is calculated every time step, it can be a costly method to process.</li>
 * <li>Behavior weights can be difficult to tweak. There have been research projects that have tried to evolve the steering
 * weights using genetic algorithms or neural networks. Results have not been encouraging, however, and manual experimentation
 * still seems to be the most sensible approach.</li>
 * <li>It's problematic with conflicting forces. For instance, a common scenario is where an agent is backed up against a wall by
 * several other agents. In this example, the separating forces from the neighboring agents can be greater than the repulsive
 * force from the wall and the agent can end up being pushed through the wall boundary. This is almost certainly not going to be
 * favorable. Sure you can make the weights for the wall avoidance huge, but then your agent may behave strangely next time it
 * finds itself alone and next to a wall.</li>
 * </ul>
 * @author davebaol
 */
public class BlendedSteering extends SteeringBehavior{

    /** The list of behaviors and their corresponding blending weights. */
    protected Array<BehaviorAndWeight> list = new Array<>();

    private SteeringAcceleration steering;

    /**
     * Creates a {@code BlendedSteering} for the specified {@code owner}, {@code maxLinearAcceleration} and
     * {@code maxAngularAcceleration}.
     * @param owner the owner of this behavior.
     */
    public BlendedSteering(Steerable owner){
        super(owner);

        this.list = new Array<>();
        this.steering = new SteeringAcceleration(newVector(owner));
    }

    /**
     * Adds a steering behavior and its weight to the list.
     * @param behavior the steering behavior to add
     * @param weight the weight of the behavior
     * @return this behavior for chaining.
     */
    public BlendedSteering add(SteeringBehavior behavior, float weight){
        return add(new BehaviorAndWeight(behavior, weight));
    }

    /**
     * Adds a steering behavior and its weight to the list.
     * @param item the steering behavior and its weight
     * @return this behavior for chaining.
     */
    public BlendedSteering add(BehaviorAndWeight item){
        item.behavior.setOwner(owner);
        list.add(item);
        return this;
    }

    /**
     * Removes a steering behavior from the list.
     * @param item the steering behavior to remove
     */
    public void remove(BehaviorAndWeight item){
        list.remove(item, true);
    }

    /**
     * Removes a steering behavior from the list.
     * @param behavior the steering behavior to remove
     */
    public void remove(SteeringBehavior behavior){
        for(int i = 0; i < list.size; i++){
            if(list.get(i).behavior == behavior){
                list.remove(i);
                return;
            }
        }
    }

    /**
     * Returns the weighted behavior at the specified index.
     * @param index the index of the weighted behavior to return
     */
    public BehaviorAndWeight get(int index){
        return list.get(index);
    }

    @Override
    protected SteeringAcceleration calculateRealSteering(SteeringAcceleration blendedSteering){
        // Clear the output to start with
        blendedSteering.setZero();

        // Go through all the behaviors
        int len = list.size;
        for(int i = 0; i < len; i++){
            BehaviorAndWeight bw = list.get(i);

            // Calculate the behavior's steering
            bw.behavior.calculateSteering(steering);

            // Scale and add the steering to the accumulator
            blendedSteering.mulAdd(steering, bw.weight);
        }

        Limiter actualLimiter = getActualLimiter();

        // Crop the result
        blendedSteering.linear.limit(actualLimiter.getMaxLinearAcceleration());
        if(blendedSteering.angular > actualLimiter.getMaxAngularAcceleration())
            blendedSteering.angular = actualLimiter.getMaxAngularAcceleration();

        return blendedSteering;
    }

    //
    // Nested classes
    //

    public static class BehaviorAndWeight{

        protected SteeringBehavior behavior;
        protected float weight;

        public BehaviorAndWeight(SteeringBehavior behavior, float weight){
            this.behavior = behavior;
            this.weight = weight;
        }

        public SteeringBehavior getBehavior(){
            return behavior;
        }

        public void setBehavior(SteeringBehavior behavior){
            this.behavior = behavior;
        }

        public float getWeight(){
            return weight;
        }

        public void setWeight(float weight){
            this.weight = weight;
        }
    }

}
