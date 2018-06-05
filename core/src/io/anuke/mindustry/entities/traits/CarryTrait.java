package io.anuke.mindustry.entities.traits;

import io.anuke.ucore.entities.trait.SolidTrait;

public interface CarryTrait extends TeamTrait, SolidTrait, TargetTrait{
    CarriableTrait getCarry();
    void setCarry(CarriableTrait unit);
    float getCarryWeight();
}
