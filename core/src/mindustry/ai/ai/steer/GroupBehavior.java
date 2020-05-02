package mindustry.ai.ai.steer;

import mindustry.ai.ai.steer.Proximity.*;

/**
 * {@code GroupBehavior} is the base class for the steering behaviors that take into consideration the agents in the game world
 * that are within the immediate area of the owner. This immediate area is defined by a {@link Proximity} that is in charge of
 * finding and processing the owner's neighbors through the given {@link ProximityCallback}.
 * @author davebaol
 */
public abstract class GroupBehavior extends SteeringBehavior{
    /** The proximity decides which agents are considered neighbors. */
    protected Proximity proximity;

    /**
     * Creates a GroupBehavior for the specified owner and proximity.
     * @param owner the owner of this behavior.
     * @param proximity the proximity to detect the owner's neighbors
     */
    public GroupBehavior(Steerable owner, Proximity proximity){
        super(owner);
        this.proximity = proximity;
    }

    /** Returns the proximity of this group behavior */
    public Proximity getProximity(){
        return proximity;
    }

    /**
     * Sets the proximity of this group behavior
     * @param proximity the proximity to set
     */
    public void setProximity(Proximity proximity){
        this.proximity = proximity;
    }

}
