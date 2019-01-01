package io.anuke.mindustry.entities.traits;

import io.anuke.arc.entities.trait.VelocityTrait;
import io.anuke.arc.util.Interval;
import io.anuke.mindustry.type.Weapon;

public interface ShooterTrait extends VelocityTrait, TeamTrait, InventoryTrait{

    Interval getTimer();

    int getShootTimer(boolean left);

    Weapon getWeapon();
}
