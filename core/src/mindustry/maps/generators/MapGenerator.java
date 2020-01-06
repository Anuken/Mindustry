package mindustry.maps.generators;

import arc.math.*;
import arc.math.geom.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

//TODO:
//- limited # of enemy spawns as filter
//- spawn loadout selection as filter
//- configure map loadout, make 1 core the default
public class MapGenerator extends Generator{
    private Map map;
    private String mapName;

    public MapGenerator(String mapName){
        this.mapName = mapName;
    }

    public void removePrefix(String name){
        this.mapName = this.mapName.substring(name.length() + 1);
    }

    public Map getMap(){
        return map;
    }

    @Override
    public void init(Schematic loadout){
        this.loadout = loadout;
        map = maps.loadInternalMap(mapName);
        width = map.width;
        height = map.height;
    }

    @Override
    public void generate(Tiles tiles){
        tiles.fill();

        SaveIO.load(map.file);

        for(Tile tile : tiles){
            if(tile.block() instanceof StorageBlock && !(tile.block() instanceof CoreBlock) && world.getZone() != null){
                for(Item item : world.getZone().resources){
                    if(Mathf.chance(0.3)){
                        tile.entity.items.add(item, Math.min(Mathf.random(500), tile.block().itemCapacity));
                    }
                }
            }
        }

        boolean anyCores = false;

        for(Tile tile : tiles){
            if(tile.overlay() == Blocks.spawn){
                int rad = 10;
                Geometry.circle(tile.x, tile.y, tiles.width(), tiles.height(), rad, (wx, wy) -> {
                    if(tile.overlay().itemDrop != null){
                        tile.clearOverlay();
                    }
                });
            }

            if(tile.block() instanceof CoreBlock && tile.getTeam() == state.rules.defaultTeam){
                schematics.placeLoadout(loadout, tile.x, tile.y);
                anyCores = true;
            }
        }

        if(!anyCores){
            throw new IllegalArgumentException("All zone maps must have a core.");
        }

        world.prepareTiles(tiles);
        world.setMap(map);
    }
}
