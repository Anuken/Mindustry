package mindustry.ai.ai.steer.limiters;

/**
 * A {@code LinearLimiter} provides the maximum magnitudes of linear speed and linear acceleration. Angular methods throw an
 * {@link UnsupportedOperationException}.
 * @author davebaol
 */
public class LinearLimiter extends NullLimiter{

    private float maxLinearAcceleration;
    private float maxLinearSpeed;

    /**
     * Creates a {@code LinearLimiter}.
     * @param maxLinearAcceleration the maximum linear acceleration
     * @param maxLinearSpeed the maximum linear speed
     */
    public LinearLimiter(float maxLinearAcceleration, float maxLinearSpeed){
        this.maxLinearAcceleration = maxLinearAcceleration;
        this.maxLinearSpeed = maxLinearSpeed;
    }

    /** Returns the maximum linear speed. */
    @Override
    public float getMaxLinearSpeed(){
        return maxLinearSpeed;
    }

    /** Sets the maximum linear speed. */
    @Override
    public void setMaxLinearSpeed(float maxLinearSpeed){
        this.maxLinearSpeed = maxLinearSpeed;
    }

    /** Returns the maximum linear acceleration. */
    @Override
    public float getMaxLinearAcceleration(){
        return maxLinearAcceleration;
    }

    /** Sets the maximum linear acceleration. */
    @Override
    public void setMaxLinearAcceleration(float maxLinearAcceleration){
        this.maxLinearAcceleration = maxLinearAcceleration;
    }

}
