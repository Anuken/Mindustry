package mindustry.ai.ai.steer.behaviors;

import mindustry.ai.ai.steer.*;

/**
 * The entire steering framework assumes that the direction a character is facing does not have to be its direction of motion. In
 * many cases, however, you would like the character to face in the direction it is moving. To do this you can manually align the
 * orientation of the character to its linear velocity on each frame update or you can use the {@code LookWhereYouAreGoing}
 * behavior.
 * <p>
 * {@code LookWhereYouAreGoing} behavior gives the owner angular acceleration to make it face in the direction it is moving. In
 * this way the owner changes facing gradually, which can look more natural, especially for aerial vehicles such as helicopters or
 * for human characters that can move sideways.
 * <p>
 * This is a process similar to the {@code Face} behavior. The target orientation is calculated using the current velocity of the
 * owner. If there is no velocity, then the target orientation is set to the current orientation. We have no preference in this
 * situation for any orientation.
 * @author davebaol
 */
public class LookWhereYouAreGoing extends ReachOrientation{

    /**
     * Creates a {@code LookWhereYouAreGoing} behavior for the specified owner.
     * @param owner the owner of this behavior.
     */
    public LookWhereYouAreGoing(Steerable owner){
        super(owner);
    }

    @Override
    protected SteeringAcceleration calculateRealSteering(SteeringAcceleration steering){
        // Check for a zero direction, and return no steering if so
        if(owner.getLinearVelocity().isZero(getActualLimiter().getZeroLinearSpeedThreshold())) return steering.setZero();

        // Calculate the orientation based on the velocity of the owner
        float orientation = owner.vectorToAngle(owner.getLinearVelocity());

        // Delegate to ReachOrientation
        return reachOrientation(steering, orientation);
    }

}
