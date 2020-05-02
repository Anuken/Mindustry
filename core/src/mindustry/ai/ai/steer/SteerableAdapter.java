package mindustry.ai.ai.steer;

import arc.math.geom.*;

/**
 * An adapter class for {@link Steerable}. You can derive from this and only override what you are interested in. For example,
 * this comes in handy when you have to create on the fly a target for a particular behavior.
 * @author davebaol
 */
public class SteerableAdapter implements Steerable{

    @Override
    public float getMaxLinearSpeed(){
        return 0;
    }

    @Override
    public void setMaxLinearSpeed(float maxLinearSpeed){
    }

    @Override
    public float getMaxLinearAcceleration(){
        return 0;
    }

    @Override
    public void setMaxLinearAcceleration(float maxLinearAcceleration){
    }

    @Override
    public float getMaxAngularSpeed(){
        return 0;
    }

    @Override
    public void setMaxAngularSpeed(float maxAngularSpeed){
    }

    @Override
    public float getMaxAngularAcceleration(){
        return 0;
    }

    @Override
    public void setMaxAngularAcceleration(float maxAngularAcceleration){
    }

    @Override
    public Vec2 getPosition(){
        return null;
    }

    @Override
    public float getOrientation(){
        return 0;
    }

    @Override
    public void setOrientation(float orientation){
    }

    @Override
    public Vec2 getLinearVelocity(){
        return null;
    }

    @Override
    public float getAngularVelocity(){
        return 0;
    }

    @Override
    public float getBoundingRadius(){
        return 0;
    }

    @Override
    public boolean isTagged(){
        return false;
    }

    @Override
    public void setTagged(boolean tagged){
    }

}
