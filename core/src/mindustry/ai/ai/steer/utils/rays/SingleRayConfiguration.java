package mindustry.ai.ai.steer.utils.rays;

import mindustry.ai.ai.steer.*;
import mindustry.ai.ai.utils.*;

/**
 * As the name suggests, a {@code SingleRayConfiguration} uses just one ray cast.
 * <p>
 * This configuration is useful in concave environments but grazes convex obstacles. It is not susceptible to the <a
 * href="../behaviors/RaycastObstacleAvoidance.html#cornerTrap">corner trap</a>, though.
 * @author davebaol
 */
public class SingleRayConfiguration extends RayConfigurationBase{

    private float length;

    /**
     * Creates a {@code SingleRayConfiguration} for the given owner where the ray has the specified length.
     * @param owner the owner of this configuration
     * @param length the length of the ray
     */
    public SingleRayConfiguration(Steerable owner, float length){
        super(owner, 1);
        this.length = length;
    }

    @Override
    public Ray[] updateRays(){
        rays[0].start.set(owner.getPosition());
        rays[0].end.set(owner.getLinearVelocity()).nor().scl(length).add(rays[0].start);
        return rays;
    }

    /** Returns the length of the ray. */
    public float getLength(){
        return length;
    }

    /** Sets the length of the ray. */
    public void setLength(float length){
        this.length = length;
    }

}
