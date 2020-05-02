package mindustry.ai.ai.steer.limiters;

/**
 * A {@code LinearAccelerationLimiter} provides the maximum magnitude of linear acceleration. All other methods throw an
 * {@link UnsupportedOperationException}.
 * @author davebaol
 */
public class LinearAccelerationLimiter extends NullLimiter{

    private float maxLinearAcceleration;

    /**
     * Creates a {@code LinearAccelerationLimiter}.
     * @param maxLinearAcceleration the maximum linear acceleration
     */
    public LinearAccelerationLimiter(float maxLinearAcceleration){
        this.maxLinearAcceleration = maxLinearAcceleration;
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
