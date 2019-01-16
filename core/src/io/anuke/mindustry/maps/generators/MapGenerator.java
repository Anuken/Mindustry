package io.anuke.mindustry.maps.generators;

import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.io.MapIO;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.maps.MapTileData;
import io.anuke.mindustry.maps.MapTileData.TileDataMarker;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.world;

public class MapGenerator extends Generator{
    private final Map map;

    public MapGenerator(String mapName){
        map = world.maps.loadInternalMap(mapName);
        width = map.meta.width;
        height = map.meta.height;
    }

    @Override
    public void generate(Tile[][] tiles){
        MapTileData data = MapIO.readTileData(map, true);

        data.position(0, 0);
        TileDataMarker marker = data.newDataMarker();

        for(int y = 0; y < data.height(); y++){
            for(int x = 0; x < data.width(); x++){
                data.read(marker);

                tiles[x][y] = new Tile(x, y, marker.floor, marker.wall == Blocks.blockpart.id ? 0 : marker.wall, marker.rotation, marker.team);
            }
        }

        world.prepareTiles(tiles);
        world.setMap(map);
    }
}
