package mindustry.ai.ai.steer.proximities;

import mindustry.ai.ai.steer.*;

/**
 * {@code InfiniteProximity} is likely the simplest type of Proximity one can imagine. All the agents contained in the specified
 * list are considered neighbors of the owner, excluded the owner itself (if it is part of the list).
 * @author davebaol
 */
public class InfiniteProximity extends ProximityBase{

    /**
     * Creates a {@code InfiniteProximity} for the specified owner and list of agents.
     * @param owner the owner of this proximity
     * @param agents the list of agents
     */
    public InfiniteProximity(Steerable owner, Iterable<? extends Steerable> agents){
        super(owner, agents);
    }

    @Override
    public int findNeighbors(ProximityCallback callback){
        int neighborCount = 0;
        for(Steerable currentAgent : agents){
            // Make sure the agent being examined isn't the owner
            if(currentAgent != owner){
                if(callback.report(currentAgent)){
                    neighborCount++;
                }
            }
        }

        return neighborCount;
    }

}
