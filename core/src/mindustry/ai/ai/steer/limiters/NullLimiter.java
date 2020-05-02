package mindustry.ai.ai.steer.limiters;

import mindustry.ai.ai.steer.*;

/**
 * A {@code NullLimiter} always throws {@link UnsupportedOperationException}. Typically it's used as the base class of partial or
 * immutable limiters.
 * @author davebaol
 */
public class NullLimiter implements Limiter{

    /**
     * An immutable limiter whose getters return {@link Float#POSITIVE_INFINITY} and setters throw
     * {@link UnsupportedOperationException}.
     */
    public static final NullLimiter NEUTRAL_LIMITER = new NullLimiter(){

        @Override
        public float getMaxLinearSpeed(){
            return Float.POSITIVE_INFINITY;
        }

        @Override
        public float getMaxLinearAcceleration(){
            return Float.POSITIVE_INFINITY;
        }

        @Override
        public float getMaxAngularSpeed(){
            return Float.POSITIVE_INFINITY;
        }

        @Override
        public float getMaxAngularAcceleration(){
            return Float.POSITIVE_INFINITY;
        }

    };

    /** Creates a {@code NullLimiter}. */
    public NullLimiter(){
    }

    /**
     * Guaranteed to throw UnsupportedOperationException.
     * @throws UnsupportedOperationException always
     */
    @Override
    public float getMaxLinearSpeed(){
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw UnsupportedOperationException.
     * @throws UnsupportedOperationException always
     */
    @Override
    public void setMaxLinearSpeed(float maxLinearSpeed){
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw UnsupportedOperationException.
     * @throws UnsupportedOperationException always
     */
    @Override
    public float getMaxLinearAcceleration(){
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw UnsupportedOperationException.
     * @throws UnsupportedOperationException always
     */
    @Override
    public void setMaxLinearAcceleration(float maxLinearAcceleration){
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw UnsupportedOperationException.
     * @throws UnsupportedOperationException always
     */
    @Override
    public float getMaxAngularSpeed(){
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw UnsupportedOperationException.
     * @throws UnsupportedOperationException always
     */
    @Override
    public void setMaxAngularSpeed(float maxAngularSpeed){
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw UnsupportedOperationException.
     * @throws UnsupportedOperationException always
     */
    @Override
    public float getMaxAngularAcceleration(){
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw UnsupportedOperationException.
     * @throws UnsupportedOperationException always
     */
    @Override
    public void setMaxAngularAcceleration(float maxAngularAcceleration){
        throw new UnsupportedOperationException();
    }

    @Override
    public float getZeroLinearSpeedThreshold(){
        return 0.001f;
    }

    /**
     * Guaranteed to throw UnsupportedOperationException.
     * @throws UnsupportedOperationException always
     */
    @Override
    public void setZeroLinearSpeedThreshold(float zeroLinearSpeedThreshold){
        throw new UnsupportedOperationException();
    }
}
