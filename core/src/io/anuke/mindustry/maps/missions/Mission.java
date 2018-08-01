package io.anuke.mindustry.maps.missions;

import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.maps.Sector;
import io.anuke.mindustry.world.Tile;

public interface Mission{
    boolean isComplete();
    String displayString();
    GameMode getMode();

    default void generate(Tile[][] tiles, Sector sector){}
}
