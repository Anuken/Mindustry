package mindustry.ai.ai.steer.limiters;

import mindustry.ai.ai.steer.*;

/**
 * A {@code FullLimiter} provides the maximum magnitudes of speed and acceleration for both linear and angular components.
 * @author davebaol
 */
public class FullLimiter implements Limiter{

    private float maxLinearAcceleration;
    private float maxLinearSpeed;
    private float maxAngularAcceleration;
    private float maxAngularSpeed;
    private float zeroLinearSpeedThreshold;

    /**
     * Creates a {@code FullLimiter}.
     * @param maxLinearAcceleration the maximum linear acceleration
     * @param maxLinearSpeed the maximum linear speed
     * @param maxAngularAcceleration the maximum angular acceleration
     * @param maxAngularSpeed the maximum angular speed
     */
    public FullLimiter(float maxLinearAcceleration, float maxLinearSpeed, float maxAngularAcceleration, float maxAngularSpeed){
        this.maxLinearAcceleration = maxLinearAcceleration;
        this.maxLinearSpeed = maxLinearSpeed;
        this.maxAngularAcceleration = maxAngularAcceleration;
        this.maxAngularSpeed = maxAngularSpeed;
    }

    @Override
    public float getMaxLinearSpeed(){
        return maxLinearSpeed;
    }

    @Override
    public void setMaxLinearSpeed(float maxLinearSpeed){
        this.maxLinearSpeed = maxLinearSpeed;
    }

    @Override
    public float getMaxLinearAcceleration(){
        return maxLinearAcceleration;
    }

    @Override
    public void setMaxLinearAcceleration(float maxLinearAcceleration){
        this.maxLinearAcceleration = maxLinearAcceleration;
    }

    @Override
    public float getMaxAngularSpeed(){
        return maxAngularSpeed;
    }

    @Override
    public void setMaxAngularSpeed(float maxAngularSpeed){
        this.maxAngularSpeed = maxAngularSpeed;
    }

    @Override
    public float getMaxAngularAcceleration(){
        return maxAngularAcceleration;
    }

    @Override
    public void setMaxAngularAcceleration(float maxAngularAcceleration){
        this.maxAngularAcceleration = maxAngularAcceleration;
    }

    @Override
    public float getZeroLinearSpeedThreshold(){
        return zeroLinearSpeedThreshold;
    }

    @Override
    public void setZeroLinearSpeedThreshold(float zeroLinearSpeedThreshold){
        this.zeroLinearSpeedThreshold = zeroLinearSpeedThreshold;
    }
}
