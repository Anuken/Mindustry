package mindustry.entities.traits;

import arc.util.Interval;
import mindustry.type.Weapon;

public interface ShooterTrait extends VelocityTrait, TeamTrait{

    Interval getTimer();

    int getShootTimer(boolean left);

    Weapon getWeapon();
}
