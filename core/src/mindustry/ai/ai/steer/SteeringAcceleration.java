package mindustry.ai.ai.steer;


import arc.math.geom.*;

/**
 * {@code SteeringAcceleration} is a movement requested by the steering system. It is made up of two components, linear and angular
 * acceleration.
 * @author davebaol
 */
public class SteeringAcceleration{
    /** The linear component of this steering acceleration. */
    public Vec2 linear;

    /** The angular component of this steering acceleration. */
    public float angular;

    /**
     * Creates a {@code SteeringAcceleration} with the given linear acceleration and zero angular acceleration.
     * @param linear The initial linear acceleration to give this SteeringAcceleration.
     */
    public SteeringAcceleration(Vec2 linear){
        this(linear, 0f);
    }

    /**
     * Creates a {@code SteeringAcceleration} with the given linear and angular components.
     * @param linear The initial linear acceleration to give this SteeringAcceleration.
     * @param angular The initial angular acceleration to give this SteeringAcceleration.
     */
    public SteeringAcceleration(Vec2 linear, float angular){
        if(linear == null) throw new IllegalArgumentException("Linear acceleration cannot be null");
        this.linear = linear;
        this.angular = angular;
    }

    /** Returns {@code true} if both linear and angular components of this steering acceleration are zero; {@code false} otherwise. */
    public boolean isZero(){
        return angular == 0 && linear.isZero();
    }

    /**
     * Zeros the linear and angular components of this steering acceleration.
     * @return this steering acceleration for chaining
     */
    public SteeringAcceleration setZero(){
        linear.setZero();
        angular = 0f;
        return this;
    }

    /**
     * Adds the given steering acceleration to this steering acceleration.
     * @param steering the steering acceleration
     * @return this steering acceleration for chaining
     */
    public SteeringAcceleration add(SteeringAcceleration steering){
        linear.add(steering.linear);
        angular += steering.angular;
        return this;
    }

    /**
     * Scales this steering acceleration by the specified scalar.
     * @param scalar the scalar
     * @return this steering acceleration for chaining
     */
    public SteeringAcceleration scl(float scalar){
        linear.scl(scalar);
        angular *= scalar;
        return this;
    }

    /**
     * First scale a supplied steering acceleration, then add it to this steering acceleration.
     * @param steering the steering acceleration
     * @param scalar the scalar
     * @return this steering acceleration for chaining
     */
    public SteeringAcceleration mulAdd(SteeringAcceleration steering, float scalar){
        linear.mulAdd(steering.linear, scalar);
        angular += steering.angular * scalar;
        return this;
    }

    /** Returns the square of the magnitude of this steering acceleration. This includes the angular component. */
    public float calculateSquareMagnitude(){
        return linear.len2() + angular * angular;
    }

    /** Returns the magnitude of this steering acceleration. This includes the angular component. */
    public float calculateMagnitude(){
        return (float)Math.sqrt(calculateSquareMagnitude());
    }
}
