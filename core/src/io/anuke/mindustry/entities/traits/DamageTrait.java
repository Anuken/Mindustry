package io.anuke.mindustry.entities.traits;

import io.anuke.mindustry.entities.type.*;

public interface DamageTrait{
    float damage();

    default void killed(Entity other){

    }
}
