package mindustry.ai.ai.steer.limiters;

/**
 * A {@code LinearSpeedLimiter} provides the maximum magnitudes of linear speed. All other methods throw an
 * {@link UnsupportedOperationException}.
 * @author davebaol
 */
public class LinearSpeedLimiter extends NullLimiter{

    private float maxLinearSpeed;

    /**
     * Creates a {@code LinearSpeedLimiter}.
     * @param maxLinearSpeed the maximum linear speed
     */
    public LinearSpeedLimiter(float maxLinearSpeed){
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

}
