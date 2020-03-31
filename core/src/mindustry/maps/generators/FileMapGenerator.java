package mindustry.maps.generators;

import arc.math.*;
import arc.math.geom.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

public class FileMapGenerator implements WorldGenerator{
    public final Map map = null;

    public FileMapGenerator(String mapName){
        //TODO doesn't work
        //this.map = maps.loadInternalMap(mapName);
    }

    @Override
    public void generate(Tiles tiles){
        if(true) throw new IllegalArgumentException("no!");
        tiles.fill();

        SaveIO.load(map.file);

        for(Tile tile : tiles){
            if(tile.block() instanceof StorageBlock && !(tile.block() instanceof CoreBlock) && state.hasSector()){
                for(Content content : state.getSector().data.resources){
                    if(content instanceof Item && Mathf.chance(0.3)){
                        tile.entity.items().add((Item)content, Math.min(Mathf.random(500), tile.block().itemCapacity));
                    }
                }
            }
        }

        boolean anyCores = false;

        for(Tile tile : tiles){
            if(tile.overlay() == Blocks.spawn){
                int rad = 10;
                Geometry.circle(tile.x, tile.y, tiles.width, tiles.height, rad, (wx, wy) -> {
                    if(tile.overlay().itemDrop != null){
                        tile.clearOverlay();
                    }
                });
            }

            if(tile.block() instanceof CoreBlock && tile.team() == state.rules.defaultTeam){
                //TODO PLACE THE LOADOUT
                //schematics.placeLoadout(loadout, tile.x, tile.y);
                anyCores = true;
            }
        }

        if(!anyCores){
            throw new IllegalArgumentException("All zone maps must have a core.");
        }

        world.prepareTiles(tiles);
        state.map = map;
    }
}
