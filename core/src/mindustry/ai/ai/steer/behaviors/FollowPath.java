package mindustry.ai.ai.steer.behaviors;

import arc.math.geom.*;
import mindustry.ai.ai.steer.*;
import mindustry.ai.ai.steer.utils.Path;
import mindustry.ai.ai.steer.utils.Path.*;
import mindustry.ai.ai.utils.*;

/**
 * {@code FollowPath} behavior produces a linear acceleration that moves the agent along the given path. First it calculates the
 * agent location based on the specified prediction time. Then it works out the position of the internal target based on the
 * location just calculated and the shape of the path. It finally uses {@link Seek seek} behavior to move the owner towards the
 * internal target position. However, if the path is open {@link Arrive arrive} behavior is used to approach path's extremities
 * when they are far less than the {@link FollowPath#decelerationRadius deceleration radius} from the internal target position.
 * <p>
 * For complex paths with sudden changes of direction the predictive behavior (i.e., with prediction time greater than 0) can
 * appear smoother than the non-predictive one (i.e., with no prediction time). However, predictive path following has the
 * downside of cutting corners when some sections of the path come close together. This cutting-corner attitude can make the
 * character miss a whole section of the path. This might not be what you want if, for example, the path represents a patrol
 * route.
 * @param <P> Type of path parameter implementing the {@link PathParam} interface
 * @author davebaol
 */
public class FollowPath<P extends PathParam> extends Arrive{

    /** The path to follow */
    protected Path<P> path;

    /** The distance along the path to generate the target. Can be negative if the owner has to move along the reverse direction. */
    protected float pathOffset;

    /** The current position on the path */
    protected P pathParam;

    /** The flag indicating whether to use {@link Arrive} behavior to approach the end of an open path. It defaults to {@code true}. */
    protected boolean arriveEnabled;

    /** The time in the future to predict the owner's position. Set it to 0 for non-predictive path following. */
    protected float predictionTime;

    private Vec2 internalTargetPosition;

    /**
     * Creates a non-predictive {@code FollowPath} behavior for the specified owner and path.
     * @param owner the owner of this behavior
     * @param path the path to be followed by the owner.
     */
    public FollowPath(Steerable owner, Path<P> path){
        this(owner, path, 0);
    }

    /**
     * Creates a non-predictive {@code FollowPath} behavior for the specified owner, path and path offset.
     * @param owner the owner of this behavior
     * @param path the path to be followed by the owner
     * @param pathOffset the distance along the path to generate the target. Can be negative if the owner is to move along the
     * reverse direction.
     */
    public FollowPath(Steerable owner, Path<P> path, float pathOffset){
        this(owner, path, pathOffset, 0);
    }

    /**
     * Creates a {@code FollowPath} behavior for the specified owner, path, path offset, maximum linear acceleration and prediction
     * time.
     * @param owner the owner of this behavior
     * @param path the path to be followed by the owner
     * @param pathOffset the distance along the path to generate the target. Can be negative if the owner is to move along the
     * reverse direction.
     * @param predictionTime the time in the future to predict the owner's position. Can be 0 for non-predictive path following.
     */
    public FollowPath(Steerable owner, Path<P> path, float pathOffset, float predictionTime){
        super(owner);
        this.path = path;
        this.pathParam = path.createParam();
        this.pathOffset = pathOffset;
        this.predictionTime = predictionTime;

        this.arriveEnabled = true;

        this.internalTargetPosition = newVector(owner);
    }

    @Override
    protected SteeringAcceleration calculateRealSteering(SteeringAcceleration steering){

        // Predictive or non-predictive behavior?
        Vec2 location = (predictionTime == 0) ?
        // Use the current position of the owner
        owner.getPosition()
        :
        // Calculate the predicted future position of the owner. We're reusing steering.linear here.
        steering.linear.set(owner.getPosition()).mulAdd(owner.getLinearVelocity(), predictionTime);

        // Find the distance from the start of the path
        float distance = path.calculateDistance(location, pathParam);

        // Offset it
        float targetDistance = distance + pathOffset;

        // Calculate the target position
        path.calculateTargetPosition(internalTargetPosition, pathParam, targetDistance);

        if(arriveEnabled && path.isOpen()){
            if(pathOffset >= 0){
                // Use Arrive to approach the last point of the path
                if(targetDistance > path.getLength() - decelerationRadius) return arrive(steering, internalTargetPosition);
            }else{
                // Use Arrive to approach the first point of the path
                if(targetDistance < decelerationRadius) return arrive(steering, internalTargetPosition);
            }
        }

        // Seek the target position
        steering.linear.set(internalTargetPosition).sub(owner.getPosition()).nor()
        .scl(getActualLimiter().getMaxLinearAcceleration());

        // No angular acceleration
        steering.angular = 0;

        // Output steering acceleration
        return steering;
    }

    /** Returns the path to follow */
    public Path<P> getPath(){
        return path;
    }

    /**
     * Sets the path followed by this behavior.
     * @param path the path to set
     * @return this behavior for chaining.
     */
    public FollowPath<P> setPath(Path<P> path){
        this.path = path;
        return this;
    }

    /** Returns the path offset. */
    public float getPathOffset(){
        return pathOffset;
    }

    /** Returns the flag indicating whether to use {@link Arrive} behavior to approach the end of an open path. */
    public boolean isArriveEnabled(){
        return arriveEnabled;
    }

    /** Returns the prediction time. */
    public float getPredictionTime(){
        return predictionTime;
    }

    /**
     * Sets the prediction time. Set it to 0 for non-predictive path following.
     * @param predictionTime the predictionTime to set
     * @return this behavior for chaining.
     */
    public FollowPath<P> setPredictionTime(float predictionTime){
        this.predictionTime = predictionTime;
        return this;
    }

    /**
     * Sets the flag indicating whether to use {@link Arrive} behavior to approach the end of an open path. It defaults to
     * {@code true}.
     * @param arriveEnabled the flag value to set
     * @return this behavior for chaining.
     */
    public FollowPath<P> setArriveEnabled(boolean arriveEnabled){
        this.arriveEnabled = arriveEnabled;
        return this;
    }

    /**
     * Sets the path offset to generate the target. Can be negative if the owner has to move along the reverse direction.
     * @param pathOffset the pathOffset to set
     * @return this behavior for chaining.
     */
    public FollowPath<P> setPathOffset(float pathOffset){
        this.pathOffset = pathOffset;
        return this;
    }

    /** Returns the current path parameter. */
    public P getPathParam(){
        return pathParam;
    }

    /** Returns the current position of the internal target. This method is useful for debug purpose. */
    public Vec2 getInternalTargetPosition(){
        return internalTargetPosition;
    }

    //
    // Setters overridden in order to fix the correct return type for chaining
    //

    @Override
    public FollowPath<P> setOwner(Steerable owner){
        this.owner = owner;
        return this;
    }

    @Override
    public FollowPath<P> setEnabled(boolean enabled){
        this.enabled = enabled;
        return this;
    }

    /**
     * Sets the limiter of this steering behavior. The given limiter must at least take care of the maximum linear speed and
     * acceleration. However the maximum linear speed is not required for a closed path.
     * @return this behavior for chaining.
     */
    @Override
    public FollowPath<P> setLimiter(Limiter limiter){
        this.limiter = limiter;
        return this;
    }

    @Override
    public FollowPath<P> setTarget(Location target){
        this.target = target;
        return this;
    }

    @Override
    public FollowPath<P> setArrivalTolerance(float arrivalTolerance){
        this.arrivalTolerance = arrivalTolerance;
        return this;
    }

    @Override
    public FollowPath<P> setDecelerationRadius(float decelerationRadius){
        this.decelerationRadius = decelerationRadius;
        return this;
    }

    @Override
    public FollowPath<P> setTimeToTarget(float timeToTarget){
        this.timeToTarget = timeToTarget;
        return this;
    }

}
