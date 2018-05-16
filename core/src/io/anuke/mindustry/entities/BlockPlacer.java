package io.anuke.mindustry.entities;

import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.game.Team;

public interface BlockPlacer {
    void addPlaceBlock(Tile tile);
    Team getTeam();
}
