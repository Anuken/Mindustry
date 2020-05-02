package mindustry.ai.ai.steer;

import mindustry.ai.ai.steer.behaviors.*;
import mindustry.ai.ai.utils.Timepiece;

/**
 * A {@code Proximity} defines an area that is used by group behaviors to find and process the owner's neighbors.
 * <p>
 * Typically (but not necessarily) different group behaviors share the same {@code Proximity} for a given owner. This allows you to
 * combine group behaviors so as to get a more complex behavior also known as emergent behavior. Emergent behavior is behavior
 * that looks complex and/or purposeful to the observer but is actually derived spontaneously from fairly simple rules. The
 * lower-level agents following the rules have no idea of the bigger picture; they are only aware of themselves and maybe a few of
 * their neighbors. A typical example of emergence is flocking behavior which is a combination of three group behaviors:
 * {@link Separation separation}, {@link Alignment alignment}, and {@link Cohesion cohesion}. The three behaviors are typically
 * combined through a {@link BlendedSteering blended steering}. This works okay but, because of the limited view distance of a
 * character, it's possible for an agent to become isolated from its flock. If this happens, it will just sit still and do
 * nothing. To prevent this from happening, you usually add in the {@link Wander wander} behavior too. This way, all the agents
 * keep moving all the time. Tweaking the magnitudes of each of the contributing behaviors will give you different effects such as
 * shoals of fish, loose swirling flocks of birds, or bustling close-knit herds of sheep.
 * <p>
 * Before a steering acceleration can be calculated for a combination of group behaviors, the neighbors must be determined and
 * processed. This is done by the {@link #findNeighbors} method and its callback argument.
 * <p>
 * Notes:
 * <ul>
 * <li>Sharing a {@code Proximity} instance among group behaviors having the same owner can save a little time determining the
 * neighbors only once from inside the {@code findNeighbors} method. Especially, {@code Proximity} implementation classes can use
 * {@link mindustry.ai.ai.utils.Timepiece#getTime() GdxAI.getTimepiece().getTime()} to calculate neighbors only once per frame (assuming delta time is
 * always greater than 0, if time has changed the frame has changed too). This means that
 * <ul>
 * <li>if you forget to {@link Timepiece#update(float) update the timepiece} on each frame the proximity instance will be
 * calculated only the very first time, which is not what you want of course.</li>
 * <li>ideally the timepiece should be updated before the proximity is updated by the {@link #findNeighbors(ProximityCallback)}
 * method.</li>
 * </ul>
 * </li>
 * <li>If you want to make sure a Proximity doesn't use as a neighbor a given agent from the list, for example the evader or the
 * owner itself, you have to implement a callback that prevents it from being considered by returning {@code false} from the method
 * {@link ProximityCallback#report(Steerable) reportNeighbor}.</li>
 * <li>If there is some efficient way of pruning potential neighbors before they are processed, the overall performance in time
 * will improve. Spatial data structures such as multi-resolution maps, quad-trees, oct-trees, and binary space partition (BSP)
 * trees can be used to get potential neighbors more efficiently. Spatial partitioning techniques are crucial when you have to
 * deal with lots of agents. Especially, if you're using Bullet or Box2d in your game, it's recommended to implement proximities
 * that exploit their methods to query the world. Both Bullet and Box2d internally use some kind of spatial partitioning.</li>
 * </ul>
 * @author davebaol
 */
public interface Proximity{

    /** Returns the owner of this proximity. */
    Steerable getOwner();

    /** Sets the owner of this proximity. */
    void setOwner(Steerable owner);

    /**
     * Finds the agents that are within the immediate area of the owner. Each of those agents is passed to the
     * {@link ProximityCallback#report(Steerable) reportNeighbor} method of the specified callback.
     * @return the number of neighbors found.
     */
    int findNeighbors(ProximityCallback callback);

    /**
     * The callback object used by a proximity to report the owner's neighbor.
     * @author davebaol
     */
    interface ProximityCallback{

        /**
         * The callback method used to report a neighbor.
         * @param neighbor the reported neighbor.
         * @return {@code true} if the given neighbor is valid; {@code false} otherwise.
         */
        boolean report(Steerable neighbor);

    }
}
