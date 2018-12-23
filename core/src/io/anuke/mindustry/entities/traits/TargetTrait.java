package io.anuke.mindustry.entities.traits;

import io.anuke.arc.entities.trait.SolidTrait;
import io.anuke.arc.entities.trait.VelocityTrait;
import io.anuke.arc.math.geom.Position;
import io.anuke.mindustry.game.Team;

/**
 * Base interface for targetable entities.
 */
public interface TargetTrait extends Position, VelocityTrait{

    boolean isDead();

    Team getTeam();

    default float getTargetVelocityX(){
        if(this instanceof SolidTrait){
            return ((SolidTrait) this).getDeltaX();
        }
        return getVelocity().x;
    }

    default float getTargetVelocityY(){
        if(this instanceof SolidTrait){
            return ((SolidTrait) this).getDeltaY();
        }
        return getVelocity().y;
    }

    /**
     * Whether this entity is a valid target.
     */
    default boolean isValid(){
        return !isDead();
    }
}
