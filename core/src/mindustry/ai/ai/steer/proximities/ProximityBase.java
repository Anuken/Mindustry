package mindustry.ai.ai.steer.proximities;

import mindustry.ai.ai.steer.*;

/**
 * {@code ProximityBase} is the base class for any concrete proximity based on an iterable collection of agents.
 * @author davebaol
 */
public abstract class ProximityBase implements Proximity{

    /** The owner of  this proximity. */
    protected Steerable owner;

    /**
     * The collection of the agents handled by this proximity.
     * <p>
     * Note that, being this field of type {@code Iterable}, you can either use java or libgdx collections. See
     * https://github.com/libgdx/gdx-ai/issues/65
     */
    protected Iterable<? extends Steerable> agents;

    /**
     * Creates a {@code ProximityBase} for the specified owner and list of agents.
     * @param owner the owner of this proximity
     * @param agents the list of agents
     */
    public ProximityBase(Steerable owner, Iterable<? extends Steerable> agents){
        this.owner = owner;
        this.agents = agents;
    }

    @Override
    public Steerable getOwner(){
        return owner;
    }

    @Override
    public void setOwner(Steerable owner){
        this.owner = owner;
    }

    /** Returns the the agents that represent potential neighbors. */
    public Iterable<? extends Steerable> getAgents(){
        return agents;
    }

    /** Sets the agents that represent potential neighbors. */
    public void setAgents(Iterable<Steerable> agents){
        this.agents = agents;
    }

}
