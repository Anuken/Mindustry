package io.anuke.mindustry.world.mapgen;

import io.anuke.mindustry.io.MapTileData;
import io.anuke.ucore.noise.RidgedPerlin;
import io.anuke.ucore.noise.Simplex;

public class ProcGen {
    public RidgedPerlin rid = new RidgedPerlin(1, 1);
    public Simplex sim = new Simplex();

    public MapTileData generate(GenProperties props){
        MapTileData data = new MapTileData(300, 300);
        for (int x = 0; x < data.width(); x++) {
            for (int y = 0; y < data.height(); y++) {

            }
        }
        return null;
    }
}
