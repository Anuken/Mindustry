package mindustry.entities.traits;

import arc.math.geom.Position;
import mindustry.game.Team;

/**
 * Base interface for targetable entities.
 */
public interface TargetTrait extends Position, VelocityTrait{

    boolean isDead();

    Team getTeam();

    default float getTargetVelocityX(){
        if(this instanceof SolidTrait){
            return ((SolidTrait)this).getDeltaX();
        }
        return velocity().x;
    }

    default float getTargetVelocityY(){
        if(this instanceof SolidTrait){
            return ((SolidTrait)this).getDeltaY();
        }
        return velocity().y;
    }

    /**
     * Whether this entity is a valid target.
     */
    default boolean isValid(){
        return !isDead();
    }
}
