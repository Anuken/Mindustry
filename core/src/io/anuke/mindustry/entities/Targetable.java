package io.anuke.mindustry.entities;

import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.game.Team;
import io.anuke.ucore.util.Position;

/**Base interface for targetable entities.*/
public interface Targetable extends Position{

    boolean isDead();
    Team getTeam();
    Vector2 getVelocity();

    /**Whether this entity is a valid target.*/
    default boolean isValid(){
        return !isDead();
    }
}
