package mindustry.ai.ai.steer.behaviors;

import arc.math.geom.*;
import mindustry.ai.ai.steer.*;
import mindustry.ai.ai.utils.*;

/**
 * {@code Face} behavior makes the owner look at its target. It delegates to the {@link ReachOrientation} behavior to perform the
 * rotation but calculates the target orientation first based on target and owner position.
 * @author davebaol
 */
public class Face extends ReachOrientation{

    /**
     * Creates a {@code Face} behavior for the specified owner.
     * @param owner the owner of this behavior.
     */
    public Face(Steerable owner){
        this(owner, null);
    }

    /**
     * Creates a {@code Face} behavior for the specified owner and target.
     * @param owner the owner of this behavior
     * @param target the target of this behavior.
     */
    public Face(Steerable owner, Location target){
        super(owner, target);
    }

    @Override
    protected SteeringAcceleration calculateRealSteering(SteeringAcceleration steering){
        return face(steering, target.getPosition());
    }

    protected SteeringAcceleration face(SteeringAcceleration steering, Vec2 targetPosition){
        // Get the direction to target
        Vec2 toTarget = steering.linear.set(targetPosition).sub(owner.getPosition());

        // Check for a zero direction, and return no steering if so
        if(toTarget.isZero(getActualLimiter().getZeroLinearSpeedThreshold())) return steering.setZero();

        // Calculate the orientation to face the target
        float orientation = owner.vectorToAngle(toTarget);

        // Delegate to ReachOrientation
        return reachOrientation(steering, orientation);
    }
}
