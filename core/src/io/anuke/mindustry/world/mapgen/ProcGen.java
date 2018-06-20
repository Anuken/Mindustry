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
    private RidgedPerlin rid = new RidgedPerlin(1, 1);
    private Simplex sim = new Simplex();
    private Simplex sim2 = new Simplex();

    public MapTileData generate(GenProperties props){
        sim.setSeed(Mathf.random(9999));
        sim2.setSeed(Mathf.random(9999));
        rid = new RidgedPerlin(Mathf.random(9999), 1);

        MapTileData data = new MapTileData(300, 300);
        TileDataMarker marker = data.newDataMarker();
        for (int x = 0; x < data.width(); x++) {
            for (int y = 0; y < data.height(); y++) {
                marker.floor = (byte)Blocks.stone.id;

                double r = sim2.octaveNoise2D(1, 0.6, 1f/70, x, y);
                double elevation = sim.octaveNoise2D(3, 0.5, 1f/70, x, y) * 4 - 1.2;
                double edgeDist = Math.max(data.width()/2, data.height()/2) - Math.max(Math.abs(x - data.width()/2), Math.abs(y - data.height()/2));

                double border = 10;

                if(edgeDist < border){
                    elevation += (border - edgeDist)/4.0;
                }

                if(r > 0.9){
                    marker.floor = (byte)Blocks.water.id;
                    elevation = 0;

                    if(r > 0.94){
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
