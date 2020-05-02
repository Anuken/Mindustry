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
    float getZeroLinearSpeedThreshold();

    /**
     * Sets the threshold below which the linear speed can be considered zero. It must be a small positive value near to zero.
     * Usually it is used to avoid updating the orientation when the velocity vector has a negligible length.
     */
    void setZeroLinearSpeedThreshold(float value);

    /** Returns the maximum linear speed. */
    float getMaxLinearSpeed();

    /** Sets the maximum linear speed. */
    void setMaxLinearSpeed(float maxLinearSpeed);

    /** Returns the maximum linear acceleration. */
    float getMaxLinearAcceleration();

    /** Sets the maximum linear acceleration. */
    void setMaxLinearAcceleration(float maxLinearAcceleration);

    /** Returns the maximum angular speed. */
    float getMaxAngularSpeed();

    /** Sets the maximum angular speed. */
    void setMaxAngularSpeed(float maxAngularSpeed);

    /** Returns the maximum angular acceleration. */
    float getMaxAngularAcceleration();

    /** Sets the maximum angular acceleration. */
    void setMaxAngularAcceleration(float maxAngularAcceleration);
}
