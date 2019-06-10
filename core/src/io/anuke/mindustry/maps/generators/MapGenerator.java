package io.anuke.mindustry.maps.generators;

import io.anuke.arc.collection.Array;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Point2;
import io.anuke.arc.util.Structs;
import io.anuke.arc.util.noise.Simplex;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.io.MapIO;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Loadout;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.mindustry.world.blocks.storage.CoreBlock;
import io.anuke.mindustry.world.blocks.storage.StorageBlock;

import static io.anuke.mindustry.Vars.defaultTeam;
import static io.anuke.mindustry.Vars.world;

public class MapGenerator extends Generator{
    private Map map;
    private String mapName;
    private Array<Decoration> decorations = Array.with(new Decoration(Blocks.stone, Blocks.rock, 0.003f));
    /** How much the landscape is randomly distorted. */
    public float distortion = 3;
    /**
     * The amount of final enemy spawns used. -1 to use everything in the map.
     * This amount of enemy spawns is selected randomly from the map.
     */
    public int enemySpawns = -1;
    /** Whether floor is distorted along with blocks. */
    public boolean distortFloor = false;

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
    public void init(Loadout loadout){
        this.loadout = loadout;
        map = world.maps.loadInternalMap(mapName);
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

        MapIO.loadMap(map);
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

        Simplex simplex = new Simplex(Mathf.random(99999));

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                final double scl = 10;
                Tile tile = tiles[x][y];
                int newX = Mathf.clamp((int)(simplex.octaveNoise2D(1, 1, 1.0 / scl, x, y) * distortion + x), 0, width - 1);
                int newY = Mathf.clamp((int)(simplex.octaveNoise2D(1, 1, 1.0 / scl, x + 9999, y + 9999) * distortion + y), 0, height - 1);

                if(((tile.block() instanceof StaticWall
                && tiles[newX][newY].block() instanceof StaticWall)
                || (tile.block() == Blocks.air && !tiles[newX][newY].block().synthetic())
                || (tiles[newX][newY].block() == Blocks.air && tile.block() instanceof StaticWall))){
                    tile.setBlock(tiles[newX][newY].block());
                }

                if(distortFloor){
                    tile.setFloor(tiles[newX][newY].floor());
                    if(tiles[newX][newY].overlay() != Blocks.spawn && tile.overlay() != Blocks.spawn){
                        tile.setOverlay(tiles[newX][newY].overlay());
                    }
                }

                for(Decoration decor : decorations){
                    if(x > 0 && y > 0 && (tiles[x - 1][y].block() == decor.wall || tiles[x][y - 1].block() == decor.wall)){
                        continue;
                    }

                    if(tile.block() == Blocks.air && !(decor.wall instanceof Floor) && tile.floor() == decor.floor && Mathf.chance(decor.chance)){
                        tile.setBlock(decor.wall);
                    }else if(tile.floor() == decor.floor && decor.wall instanceof Floor && Mathf.chance(decor.chance)){
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

        loadout.setup(core.x, core.y);

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
