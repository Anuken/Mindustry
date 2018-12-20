package io.anuke.mindustry.entities.traits;

import io.anuke.mindustry.type.Weapon;
import io.anuke.arc.entities.trait.VelocityTrait;
import io.anuke.arc.util.Timer;

public interface ShooterTrait extends VelocityTrait, TeamTrait, InventoryTrait{

    Timer getTimer();

    int getShootTimer(boolean left);

    Weapon getWeapon();
}
