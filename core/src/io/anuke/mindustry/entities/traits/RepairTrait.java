package io.anuke.mindustry.entities.traits;

import io.anuke.ucore.entities.trait.HealthTrait;

//TODO implement
public interface RepairTrait extends TeamTrait {

    HealthTrait getRepairing();

    void setRepairing(HealthTrait trait);

    default void drawRepair(){
        if(getRepairing() == null) return;
    }

    default void updateRepair(){
        if(getRepairing() == null) return;
    }
}
