package mindustry.ai.ai.steer.limiters;

/**
 * An {@code AngularLimiter} provides the maximum magnitudes of angular speed and angular acceleration. Linear methods throw an
 * {@link UnsupportedOperationException}.
 * @author davebaol
 */
public class AngularLimiter extends NullLimiter{

    private float maxAngularAcceleration;
    private float maxAngularSpeed;

    /**
     * Creates an {@code AngularLimiter}.
     * @param maxAngularAcceleration the maximum angular acceleration
     * @param maxAngularSpeed the maximum angular speed
     */
    public AngularLimiter(float maxAngularAcceleration, float maxAngularSpeed){
        this.maxAngularAcceleration = maxAngularAcceleration;
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
