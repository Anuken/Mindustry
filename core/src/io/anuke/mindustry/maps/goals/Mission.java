package io.anuke.mindustry.maps.goals;

import io.anuke.mindustry.maps.Sector;
import io.anuke.mindustry.world.Tile;

public interface Mission{
    boolean isComplete();

    default void generate(Tile[][] tiles, Sector sector){}
}
