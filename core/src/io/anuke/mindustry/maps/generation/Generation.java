package io.anuke.mindustry.maps.generation;

import io.anuke.mindustry.maps.Sector;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.util.SeedRandom;

public class Generation{
    public final Sector sector;
    public final Tile[][] tiles;
    public final int width, height;
    public final SeedRandom random;

    public Generation(Sector sector, Tile[][] tiles, int width, int height, SeedRandom random){
        this.sector = sector;
        this.tiles = tiles;
        this.width = width;
        this.height = height;
        this.random = random;
    }
}
