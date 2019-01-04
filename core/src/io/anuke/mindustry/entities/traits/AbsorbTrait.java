package io.anuke.mindustry.entities.traits;

import io.anuke.arc.entities.trait.DamageTrait;
import io.anuke.arc.entities.trait.Entity;

public interface AbsorbTrait extends Entity, TeamTrait, DamageTrait{
    void absorb();

    default boolean canBeAbsorbed(){
        return true;
    }

    default float getShieldDamage(){
        return getDamage();
    }
}
