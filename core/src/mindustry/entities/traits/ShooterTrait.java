package mindustry.entities.traits;

import mindustry.entities.*;

public interface ShooterTrait extends VelocityTrait, TeamTrait{
    Weapons getWeapons();
}
