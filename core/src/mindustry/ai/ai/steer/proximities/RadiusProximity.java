package mindustry.ai.ai.steer.proximities;

import arc.math.geom.*;
import mindustry.ai.ai.steer.*;
import mindustry.ai.ai.utils.Timepiece;

/**
 * A {@code RadiusProximity} elaborates any agents contained in the specified list that are within the radius of the owner.
 * <p>
 * Note that this implementation checks the AI time of the current frame through the {@link mindustry.ai.ai.utils.Timepiece#getTime()
 * GdxAI.getTimepiece().getTime()} method in order to calculate neighbors only once per frame (assuming delta time is always
 * greater than 0, if time has changed the frame has changed too). This means that
 * <ul>
 * <li>if you forget to {@link mindustry.ai.ai.utils.Timepiece#update(float) update the timepiece} on each frame the proximity instance will be
 * calculated only the very first time, which is not what you want of course.</li>
 * <li>ideally the timepiece should be updated before the proximity is updated by the {@link #findNeighbors(ProximityCallback)}
 * method.</li>
 * </ul>
 * @author davebaol
 */
public class RadiusProximity extends ProximityBase{

    /** The radius of this proximity. */
    protected float radius;

    private float lastTime;

    /**
     * Creates a {@code RadiusProximity} for the specified owner, agents and radius.
     * @param owner the owner of this proximity
     * @param agents the agents
     * @param radius the radius of the cone area
     */
    public RadiusProximity(Steerable owner, Iterable<? extends Steerable> agents, float radius){
        super(owner, agents);
        this.radius = radius;
        this.lastTime = 0;
    }

    /** Returns the radius of this proximity. */
    public float getRadius(){
        return radius;
    }

    /** Sets the radius of this proximity. */
    public void setRadius(float radius){
        this.radius = radius;
    }

    @Override
    public int findNeighbors(ProximityCallback callback){
        int neighborCount = 0;

        // If the frame is new then avoid repeating calculations
        // when this proximity is used by multiple group behaviors.
        float currentTime = Timepiece.time;
        if(this.lastTime != currentTime){
            // Save the current time
            this.lastTime = currentTime;

            Vec2 ownerPosition = owner.getPosition();

            // Scan the agents searching for neighbors
            for(Steerable currentAgent : agents){
                // Make sure the agent being examined isn't the owner
                if(currentAgent != owner){
                    float squareDistance = ownerPosition.dst2(currentAgent.getPosition());

                    // The bounding radius of the current agent is taken into account
                    // by adding it to the range
                    float range = radius + currentAgent.getBoundingRadius();

                    // If the current agent is within the range, report it to the callback
                    // and tag it for further consideration.
                    if(squareDistance < range * range){
                        if(callback.reportNeighbor(currentAgent)){
                            currentAgent.setTagged(true);
                            neighborCount++;
                            continue;
                        }
                    }
                }

                // Clear the tag
                currentAgent.setTagged(false);
            }
        }else{
            // Scan the agents searching for tagged neighbors
            for(Steerable currentAgent : agents){
                // Make sure the agent being examined isn't the owner and that
                // it's tagged.
                if(currentAgent != owner && currentAgent.isTagged()){

                    if(callback.reportNeighbor(currentAgent)){
                        neighborCount++;
                    }
                }
            }
        }

        return neighborCount;
    }

}
