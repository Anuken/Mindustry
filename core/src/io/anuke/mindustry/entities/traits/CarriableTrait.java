package io.anuke.mindustry.entities.traits;

import io.anuke.ucore.entities.trait.SolidTrait;

public interface CarriableTrait extends TeamTrait, TargetTrait, SolidTrait{

    default boolean isCarried(){
        return getCarrier() != null;
    }

    CarryTrait getCarrier();

    void setCarrier(CarryTrait carrier);
}
