package mindustry.ai.ai.steer.limiters;

/**
 * An {@code AngularSpeedLimiter} provides the maximum magnitudes of angular speed. All other methods throw an
 * {@link UnsupportedOperationException}.
 * @author davebaol
 */
public class AngularSpeedLimiter extends NullLimiter{

    private float maxAngularSpeed;

    /**
     * Creates an {@code AngularSpeedLimiter}.
     * @param maxAngularSpeed the maximum angular speed
     */
    public AngularSpeedLimiter(float maxAngularSpeed){
        this.maxAngularSpeed = maxAngularSpeed;
    }

    /** Returns the maximum angular speed. */
    @Override
    public float getMaxAngularSpeed(){
        return maxAngularSpeed;
    }

    /** Sets the maximum angular speed. */
    @Override
    public void setMaxAngularSpeed(float maxAngularSpeed){
        this.maxAngularSpeed = maxAngularSpeed;
    }

}
