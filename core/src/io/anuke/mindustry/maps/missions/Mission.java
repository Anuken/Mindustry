package io.anuke.mindustry.maps.missions;

import io.anuke.mindustry.maps.Sector;
import io.anuke.mindustry.world.Tile;

public interface Mission{
    boolean isComplete();
    String displayString();

    default void generate(Tile[][] tiles, Sector sector){}
}
