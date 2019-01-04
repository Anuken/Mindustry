package io.anuke.mindustry.entities.traits;

import io.anuke.mindustry.game.Team;
import io.anuke.arc.entities.trait.Entity;

public interface TeamTrait extends Entity{
    Team getTeam();
}
