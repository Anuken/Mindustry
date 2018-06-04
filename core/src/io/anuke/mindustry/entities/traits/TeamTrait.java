package io.anuke.mindustry.entities.traits;

import io.anuke.mindustry.game.Team;
import io.anuke.ucore.entities.component.Entity;

public interface TeamTrait extends Entity {
    Team getTeam();
}
