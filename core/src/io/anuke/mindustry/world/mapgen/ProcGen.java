package io.anuke.mindustry.world.mapgen;

import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.content.blocks.StorageBlocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.MapTileData;
import io.anuke.mindustry.io.MapTileData.DataPosition;
import io.anuke.mindustry.io.MapTileData.TileDataMarker;
import io.anuke.ucore.noise.RidgedPerlin;
import io.anuke.ucore.noise.Simplex;
import io.anuke.ucore.util.Bits;
import io.anuke.ucore.util.Mathf;

public class ProcGen {
    public RidgedPerlin rid = new RidgedPerlin(1, 1);
    public Simplex sim = new Simplex();

    public MapTileData generate(GenProperties props){
        sim.setSeed(Mathf.random(9999));
        rid = new RidgedPerlin(Mathf.random(9999), 1);

        MapTileData data = new MapTileData(300, 300);
        TileDataMarker marker = data.newDataMarker();
        for (int x = 0; x < data.width(); x++) {
            for (int y = 0; y < data.height(); y++) {
                marker.floor = (byte)Blocks.stone.id;

                double r = rid.getValue(x, y, 1/70f);
                double elevation = sim.octaveNoise2D(3, 0.6, 1f/50, x, y) * 3 - 1 - r*2;

                if(r > 0.0){
                    marker.floor = (byte)Blocks.water.id;
                    elevation = 0;

                    if(r > 0.055){
                        marker.floor = (byte)Blocks.deepwater.id;
                    }
                }

                marker.elevation = (byte)Math.max(elevation, 0);

                data.write(marker);
            }
        }
        data.write(data.width()/2, data.height()/2, DataPosition.wall, (byte)StorageBlocks.core.id);
        data.write(data.width()/2, data.height()/2, DataPosition.rotationTeam, Bits.packByte((byte)0, (byte)Team.blue.ordinal()));
        return data;
    }
}
