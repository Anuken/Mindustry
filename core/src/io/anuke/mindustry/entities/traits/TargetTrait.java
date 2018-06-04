package io.anuke.mindustry.entities.traits;

import io.anuke.mindustry.game.Team;
import io.anuke.ucore.entities.trait.VelocityTrait;
import io.anuke.ucore.entities.trait.PosTrait;

/**Base interface for targetable entities.*/
public interface TargetTrait extends PosTrait, VelocityTrait {

    boolean isDead();
    Team getTeam();

    /**Whether this entity is a valid target.*/
    default boolean isValid(){
        return !isDead();
    }
}
