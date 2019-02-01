package io.anuke.mindustry.maps.generators;

import io.anuke.arc.collection.Array;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Point2;
import io.anuke.arc.util.Structs;
import io.anuke.arc.util.noise.Simplex;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.MapIO;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.maps.MapTileData;
import io.anuke.mindustry.maps.MapTileData.TileDataMarker;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.mindustry.world.blocks.OreBlock;
import io.anuke.mindustry.world.blocks.StaticWall;
import io.anuke.mindustry.world.blocks.storage.CoreBlock;
import io.anuke.mindustry.world.blocks.storage.StorageBlock;

import static io.anuke.mindustry.Vars.*;

public class MapGenerator extends Generator{
    private Map map;
    private String mapName;
    private Array<Decoration> decorations = new Array<>();
    /**How much the landscape is randomly distorted.*/
    public float distortion = 3;
    /**The amount of final enemy spawns used. -1 to use everything in the map.
     * This amount of enemy spawns is selected randomly from the map.*/
    public int enemySpawns = -1;
    /*Whether floor is distorted along with blocks.*/
    public boolean distortFloor = false;
    /**Items randomly added to containers and vaults.*/
    public ItemStack[] storageDrops = ItemStack.with(Items.copper, 300, Items.lead, 300, Items.silicon, 200, Items.graphite, 200, Items.blastCompound, 200);

    public MapGenerator(String mapName){
        this.mapName = mapName;
    }

    public MapGenerator(String mapName, int enemySpawns){
        this.mapName = mapName;
        this.enemySpawns = enemySpawns;
    }

    public MapGenerator decor(Decoration... decor){
        this.decorations.addAll(decor);
        return this;
    }

    public MapGenerator dist(float distortion){
        this.distortion = distortion;
        return this;
    }

    public MapGenerator dist(float distortion, boolean floor){
        this.distortion = distortion;
        this.distortFloor = floor;
        return this;
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
        Array<Block> coreTypes = new Array<>();
        Array<Point2> enemies = new Array<>();

        for(int y = 0; y < data.height(); y++){
            for(int x = 0; x < data.width(); x++){
                data.read(marker);

                if(content.block(marker.wall) instanceof CoreBlock){
                    players.add(new Point2(x, y));
                    coreTypes.add(content.block(marker.wall));
                    marker.wall = 0;
                }

                if(enemySpawns != -1 && content.block(marker.wall) == Blocks.spawn){
                    enemies.add(new Point2(x, y));
                    marker.wall = 0;
                }

                tiles[x][y] = new Tile(x, y, marker.floor, marker.wall == Blocks.part.id ? 0 : marker.wall, marker.rotation, marker.team);
            }
        }

        Simplex simplex = new Simplex(Mathf.random(99999));

        for(int x = 0; x < data.width(); x++){
            for(int y = 0; y < data.height(); y++){
                final double scl = 10;
                Tile tile = tiles[x][y];
                int newX = Mathf.clamp((int)(simplex.octaveNoise2D(1, 1, 1.0 / scl, x, y) * distortion + x), 0, data.width()-1);
                int newY = Mathf.clamp((int)(simplex.octaveNoise2D(1, 1, 1.0 / scl, x + 9999, y + 9999) * distortion + y), 0, data.height()-1);

                if((tile.block() instanceof StaticWall
                    && tiles[newX][newY].block() instanceof StaticWall)
                    || (tile.block() == Blocks.air && !tiles[newX][newY].block().synthetic())
                    || (tiles[newX][newY].block() == Blocks.air && tile.block() instanceof StaticWall)){
                    tile.setBlock(tiles[newX][newY].block());
                }

                if(distortFloor){
                    tile.setFloor(tiles[newX][newY].floor());
                }

                for(Decoration decor : decorations){
                    if(tile.block() == Blocks.air && !(decor.wall instanceof Floor) && tile.floor() == decor.floor && Mathf.chance(decor.chance)){
                        tile.setBlock(decor.wall);
                    }else if(tile.floor() == decor.floor && decor.wall instanceof Floor && Mathf.chance(decor.chance)){
                        tile.setFloor((Floor)decor.wall);
                    }
                }

                if(tile.getTeam() == Team.none && tile.block() instanceof StorageBlock){
                    for(ItemStack stack : storageDrops){
                        if(Mathf.chance(0.3)){
                            tile.entity.items.add(stack.item, Mathf.random(stack.amount));
                        }
                    }
                }
            }
        }

        if(enemySpawns != -1){
            if(enemySpawns > enemies.size){
                throw new IllegalArgumentException("Enemy spawn pool greater than map spawn number.");
            }

            enemies.shuffle();
            for(int i = 0; i < enemySpawns; i++){
                Point2 point = enemies.get(i);
                tiles[point.x][point.y].setBlock(Blocks.spawn);

                int rad = 10, frad = 12;

                for(int x = -rad; x <= rad; x++){
                    for(int y = -rad; y <= rad; y++){
                        int wx = x + point.x, wy = y + point.y;
                        double dst = Mathf.dst(x, y);
                        if(dst < frad && Structs.inBounds(wx, wy, tiles) && (dst <= rad || Mathf.chance(0.5))){
                            Tile tile = tiles[wx][wy];
                            if(tile.floor() instanceof OreBlock){
                                OreBlock block = (OreBlock)tile.floor();
                                tile.setFloor(block.base);
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

        //TODO set specific core block?
        tiles[core.x][core.y].setBlock(coreTypes.get(players.indexOf(core)), defaultTeam);

        world.prepareTiles(tiles);
        world.setMap(map);
    }

    public static class Decoration{
        public final Block floor;
        public final Block wall;
        public final double chance;

        public Decoration(Block floor, Block wall, double chance){
            this.floor = floor;
            this.wall = wall;
            this.chance = chance;
        }
    }
}
