package mindustry.ai.ai.steer;

/**
 * A {@code Limiter} provides the maximum magnitudes of speed and acceleration for both linear and angular components.
 * @author davebaol
 */
public interface Limiter{

    /**
     * Returns the threshold below which the linear speed can be considered zero. It must be a small positive value near to zero.
     * Usually it is used to avoid updating the orientation when the velocity vector has a negligible length.
     */
    default float getZeroLinearSpeedThreshold(){
        return 0.001f;
    }

    /** Returns the maximum linear speed. */
    default float getMaxLinearSpeed(){
        return Float.MAX_VALUE;
    }

    /** Returns the maximum linear acceleration. */
    default float getMaxLinearAcceleration(){
        return Float.MAX_VALUE;
    }

    /** Returns the maximum angular speed. */
    default float getMaxAngularSpeed(){
        return Float.MAX_VALUE;
    }

    /** Returns the maximum angular acceleration. */
    default float getMaxAngularAcceleration(){
        return Float.MAX_VALUE;
    }
}
