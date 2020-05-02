package mindustry.ai.ai.steer.limiters;

/**
 * An {@code AngularAccelerationLimiter} provides the maximum magnitude of angular acceleration. All other methods throw an
 * {@link UnsupportedOperationException}.
 * @author davebaol
 */
public class AngularAccelerationLimiter extends NullLimiter{

    private float maxAngularAcceleration;

    /**
     * Creates an {@code AngularAccelerationLimiter}.
     * @param maxAngularAcceleration the maximum angular acceleration
     */
    public AngularAccelerationLimiter(float maxAngularAcceleration){
        this.maxAngularAcceleration = maxAngularAcceleration;
    }

    /** Returns the maximum angular acceleration. */
    @Override
    public float getMaxAngularAcceleration(){
        return maxAngularAcceleration;
    }

    /** Sets the maximum angular acceleration. */
    @Override
    public void setMaxAngularAcceleration(float maxAngularAcceleration){
        this.maxAngularAcceleration = maxAngularAcceleration;
    }

}
