package mindustry.ai.ai.steer.utils.rays;

import mindustry.ai.ai.steer.*;
import mindustry.ai.ai.steer.utils.*;
import mindustry.ai.ai.utils.*;

/**
 * {@code RayConfigurationBase} is the base class for concrete ray configurations having a fixed number of rays.
 * @author davebaol
 */
public abstract class RayConfigurationBase implements RayConfiguration{

    protected Steerable owner;
    protected Ray[] rays;

    /**
     * Creates a {@code RayConfigurationBase} for the given owner and the specified number of rays.
     * @param owner the owner of this configuration
     * @param numRays the number of rays used by this configuration
     */
    public RayConfigurationBase(Steerable owner, int numRays){
        this.owner = owner;
        this.rays = new Ray[numRays];
        for(int i = 0; i < numRays; i++)
            this.rays[i] = new Ray(owner.getPosition().cpy().setZero(), owner.getPosition().cpy().setZero());
    }

    /** Returns the owner of this configuration. */
    public Steerable getOwner(){
        return owner;
    }

    /** Sets the owner of this configuration. */
    public void setOwner(Steerable owner){
        this.owner = owner;
    }

    /** Returns the rays of this configuration. */
    public Ray[] getRays(){
        return rays;
    }

    /** Sets the rays of this configuration. */
    public void setRays(Ray[] rays){
        this.rays = rays;
    }

}
