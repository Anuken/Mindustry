package io.anuke.mindustry.maps.generators;

import io.anuke.arc.collection.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.io.*;
import io.anuke.mindustry.maps.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.mindustry.world.blocks.storage.*;

import static io.anuke.mindustry.Vars.*;

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
    public void generate(Tile[][] tiles){
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                tiles[x][y] = new Tile(x, y);
            }
        }

        SaveIO.load(map.file);
        Array<Point2> players = new Array<>();
        Array<Point2> enemies = new Array<>();

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                if(tiles[x][y].block() instanceof CoreBlock && tiles[x][y].getTeam() == defaultTeam){
                    players.add(new Point2(x, y));
                    tiles[x][y].setBlock(Blocks.air);
                }

                if(tiles[x][y].overlay() == Blocks.spawn && enemySpawns != -1){
                    enemies.add(new Point2(x, y));
                    tiles[x][y].setOverlay(Blocks.air);
                }

                if(tiles[x][y].block() instanceof BlockPart){
                    tiles[x][y].setBlock(Blocks.air);
                }
            }
        }

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                Tile tile = tiles[x][y];

                for(Decoration decor : decorations){
                    if(x > 0 && y > 0 && (tiles[x - 1][y].block() == decor.wall || tiles[x][y - 1].block() == decor.wall)){
                        continue;
                    }

                    if(tile.block() == Blocks.air && !(decor.wall instanceof Floor) && tile.floor() == decor.floor && Mathf.chance(decor.chance)){
                        tile.setBlock(decor.wall);
                    }else if(tile.floor() == decor.floor && decor.wall.isOverlay() && Mathf.chance(decor.chance)){
                        tile.setOverlay(decor.wall);
                    }else if(tile.floor() == decor.floor && decor.wall.isFloor() && !decor.wall.isOverlay() && Mathf.chance(decor.chance)){
                        tile.setFloor((Floor)decor.wall);
                    }
                }

                if(tile.block() instanceof StorageBlock && !(tile.block() instanceof CoreBlock) && world.getZone() != null){
                    for(Item item : world.getZone().resources){
                        if(Mathf.chance(0.3)){
                            tile.entity.items.add(item, Math.min(Mathf.random(500), tile.block().itemCapacity));
                        }
                    }
                }
            }
        }

        if(enemySpawns != -1){
            if(enemySpawns > enemies.size){
                throw new IllegalArgumentException("Enemy spawn pool greater than map spawn number for map: " + mapName);
            }

            enemies.shuffle();
            for(int i = 0; i < enemySpawns; i++){
                Point2 point = enemies.get(i);
                tiles[point.x][point.y].setOverlay(Blocks.spawn);

                int rad = 10, frad = 12;

                for(int x = -rad; x <= rad; x++){
                    for(int y = -rad; y <= rad; y++){
                        int wx = x + point.x, wy = y + point.y;
                        double dst = Mathf.dst(x, y);
                        if(dst < frad && Structs.inBounds(wx, wy, tiles) && (dst <= rad || Mathf.chance(0.5))){
                            Tile tile = tiles[wx][wy];
                            if(tile.overlay() != Blocks.spawn){
                                tile.clearOverlay();
                            }
                        }
                    }
                }
            }
        }

        Point2 core = players.random();
        if(core == null){
            throw new IllegalArgumentException("All zone maps must have a core.");
        }

        schematics.placeLoadout(loadout, core.x, core.y);

        world.prepareTiles(tiles);
        world.setMap(map);
    }
}
