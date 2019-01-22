package io.anuke.mindustry.maps.generators;

import io.anuke.arc.collection.Array;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Point2;
import io.anuke.arc.util.noise.Simplex;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.io.MapIO;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.maps.MapTileData;
import io.anuke.mindustry.maps.MapTileData.TileDataMarker;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.storage.CoreBlock;

import static io.anuke.mindustry.Vars.*;

public class MapGenerator extends Generator{
    private Map map;
    private String mapName;

    /**The amount of final enemy spawns used. -1 to use everything in the map.
     * This amount of enemy spawns is selected randomly from the map.*/
    public int enemySpawns = -1;

    public MapGenerator(String mapName){
        this.mapName = mapName;
    }

    public MapGenerator(String mapName, int enemySpawns){
        this.mapName = mapName;
        this.enemySpawns = enemySpawns;
    }

    @Override
    public void init(){
        map = world.maps.loadInternalMap(mapName);
        width = map.meta.width;
        height = map.meta.height;
    }

    @Override
    public void generate(Tile[][] tiles){
        MapTileData data = MapIO.readTileData(map, true);

        data.position(0, 0);
        TileDataMarker marker = data.newDataMarker();
        Array<Point2> players = new Array<>();
        Array<Point2> enemies = new Array<>();

        for(int y = 0; y < data.height(); y++){
            for(int x = 0; x < data.width(); x++){
                data.read(marker);

                if(content.block(marker.wall) instanceof CoreBlock){
                    players.add(new Point2(x, y));
                    marker.wall = 0;
                }

                if(enemySpawns != -1 && content.block(marker.wall) == Blocks.spawn){
                    enemies.add(new Point2(x, y));
                    marker.wall = 0;
                }

                tiles[x][y] = new Tile(x, y, marker.floor, marker.wall == Blocks.blockpart.id ? 0 : marker.wall, marker.rotation, marker.team);
            }
        }

        Simplex simplex = new Simplex(Mathf.random(99999));

        for(int x = 0; x < data.width(); x++){
            for(int y = 0; y < data.height(); y++){
                final double scl = 10;
                final int mag = 3;
                int newX = Mathf.clamp((int)(simplex.octaveNoise2D(1, 1, 1.0 / scl, x, y) * mag + x), 0, data.width()-1);
                int newY = Mathf.clamp((int)(simplex.octaveNoise2D(1, 1, 1.0 / scl, x + 9999, y + 9999) * mag + y), 0, data.height()-1);
                if(tiles[newX][newY].block() != Blocks.spawn){
                    tiles[x][y].setBlock(tiles[newX][newY].block());
                }
            }
        }

        if(enemySpawns > enemies.size){
            throw new IllegalArgumentException("Enemy spawn pool greater than map spawn number.");
        }

        if(enemySpawns != -1){
            enemies.shuffle();
            for(int i = 0; i < enemySpawns; i++){
                Point2 point = enemies.get(i);
                tiles[point.x][point.y].setBlock(Blocks.spawn);
            }
        }

        Point2 core = players.random();
        if(core == null){
            throw new IllegalArgumentException("All zone maps must have a core.");
        }

        //TODO set specific core block?
        tiles[core.x][core.y].setBlock(Blocks.core, defaultTeam);

        world.prepareTiles(tiles);
        world.setMap(map);
    }
}
