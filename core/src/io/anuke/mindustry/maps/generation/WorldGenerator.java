package io.anuke.mindustry.maps.generation;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.content.blocks.OreBlocks;
import io.anuke.mindustry.content.blocks.StorageBlocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.maps.MapTileData;
import io.anuke.mindustry.maps.MapTileData.TileDataMarker;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.ucore.noise.RidgedPerlin;
import io.anuke.ucore.noise.Simplex;
import io.anuke.ucore.noise.VoronoiNoise;
import io.anuke.ucore.util.Bits;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.SeedRandom;

import static io.anuke.mindustry.Vars.*;


public class WorldGenerator{
    private final int seed = 0;
    private int oreIndex = 0;

    private Simplex sim = new Simplex(seed);
    private Simplex sim2 = new Simplex(seed + 1);
    private Simplex sim3 = new Simplex(seed + 2);
    private VoronoiNoise vn = new VoronoiNoise(seed + 2, (short)0);

    private SeedRandom random = new SeedRandom(seed + 3);

    private GenResult result = new GenResult();
    private ObjectMap<Block, Block> decoration;

    public WorldGenerator(){
        decoration = Mathf.map(
            Blocks.grass, Blocks.shrub,
            Blocks.stone, Blocks.rock,
            Blocks.ice, Blocks.icerock,
            Blocks.snow, Blocks.icerock,
            Blocks.blackstone, Blocks.blackrock
        );
    }

    /**Loads raw map tile data into a Tile[][] array, setting up multiblocks, cliffs and ores. */
    public void loadTileData(Tile[][] tiles, MapTileData data, boolean genOres, int seed){
        data.position(0, 0);
        TileDataMarker marker = data.newDataMarker();

        for(int y = 0; y < data.height(); y++){
            for(int x = 0; x < data.width(); x++){
                data.read(marker);

                tiles[x][y] = new Tile(x, y, marker.floor, marker.wall == Blocks.blockpart.id ? 0 : marker.wall, marker.rotation, marker.team, marker.elevation);
            }
        }

        prepareTiles(tiles, seed, genOres);
    }

    public void prepareTiles(Tile[][] tiles, int seed, boolean genOres){

        //find multiblocks
        IntArray multiblocks = new IntArray();

        for(int x = 0; x < tiles.length; x++){
            for(int y = 0; y < tiles[0].length; y++){
                Tile tile = tiles[x][y];

                Team team = tile.getTeam();

                if(tile.block() == StorageBlocks.core &&
                        state.teams.has(team)){
                    state.teams.get(team).cores.add(tile);
                }

                if(tiles[x][y].block().isMultiblock()){
                    multiblocks.add(tiles[x][y].packedPosition());
                }
            }
        }

        //place multiblocks now
        for(int i = 0; i < multiblocks.size; i++){
            int pos = multiblocks.get(i);

            int x = pos % tiles.length;
            int y = pos / tiles[0].length;

            Block result = tiles[x][y].block();
            Team team = tiles[x][y].getTeam();

            int offsetx = -(result.size - 1) / 2;
            int offsety = -(result.size - 1) / 2;

            for(int dx = 0; dx < result.size; dx++){
                for(int dy = 0; dy < result.size; dy++){
                    int worldx = dx + offsetx + x;
                    int worldy = dy + offsety + y;
                    if(!(worldx == x && worldy == y)){
                        Tile toplace = world.tile(worldx, worldy);
                        if(toplace != null){
                            toplace.setLinked((byte) (dx + offsetx), (byte) (dy + offsety));
                            toplace.setTeam(team);
                        }
                    }
                }
            }
        }

        //update cliffs, occlusion data
        for(int x = 0; x < tiles.length; x++){
            for(int y = 0; y < tiles[0].length; y++){
                Tile tile = tiles[x][y];

                tile.updateOcclusion();

                //fix things on cliffs that shouldn't be
                if(tile.block() != Blocks.air && tile.cliffs != 0){
                    tile.setBlock(Blocks.air);
                }
            }
        }

        oreIndex = 0;

        if(genOres){
            Array<OreEntry> ores = Array.with(
                    new OreEntry(Items.tungsten, 0.3f, seed),
                    new OreEntry(Items.coal, 0.284f, seed),
                    new OreEntry(Items.lead, 0.28f, seed),
                    new OreEntry(Items.titanium, 0.27f, seed),
                    new OreEntry(Items.thorium, 0.26f, seed)
            );

            for(int x = 0; x < tiles.length; x++){
                for(int y = 0; y < tiles[0].length; y++){

                    Tile tile = tiles[x][y];

                    if(!tile.floor().hasOres || tile.cliffs != 0 || tile.block() != Blocks.air){
                        continue;
                    }

                    for(int i = ores.size - 1; i >= 0; i--){
                        OreEntry entry = ores.get(i);
                        if(entry.noise.octaveNoise2D(1, 0.7, 1f / (4 + i * 2), x, y) / 4f +
                                Math.abs(0.5f - entry.noise.octaveNoise2D(2, 0.7, 1f / (50 + i * 2), x, y)) > 0.48f &&
                                Math.abs(0.5f - entry.noise.octaveNoise2D(1, 1, 1f / (55 + i * 4), x, y)) > 0.22f){
                            tile.setFloor((Floor) OreBlocks.get(tile.floor(), entry.item));
                            break;
                        }
                    }
                }
            }
        }
    }

    public void generateMap(Tile[][] tiles, int sectorX, int sectorY){
        int width = tiles.length, height = tiles[0].length;

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                GenResult result = generateTile(sectorX, sectorY, x, y);
                Tile tile = new Tile(x, y, (byte)result.floor.id, (byte)result.wall.id, (byte)0, (byte)0, result.elevation);
                tiles[x][y] = tile;
            }
        }

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                Tile tile = tiles[x][y];

                byte elevation = tile.getElevation();

                for(GridPoint2 point : Geometry.d4){
                    if(!Mathf.inBounds(x + point.x, y + point.y, width, height)) continue;
                    if(tiles[x + point.x][y + point.y].getElevation() < elevation){

                        if(Mathf.chance(0.05)){
                            tile.setElevation(-1);
                        }
                        break;
                    }
                }
            }
        }

        int coreX = width/2, coreY = height/3;

        tiles[coreX][coreY].setBlock(StorageBlocks.core);
        tiles[coreX][coreY].setTeam(Team.blue);

        prepareTiles(tiles, seed, true);
    }

    public void setSector(int sectorX, int sectorY){
        random.setSeed(Bits.packLong(sectorX, sectorY));
    }

    public GenResult generateTile(int sectorX, int sectorY, int localX, int localY){
        return generateTile(sectorX, sectorY, localX, localY, true);
    }

    public GenResult generateTile(int sectorX, int sectorY, int localX, int localY, boolean detailed){
        int x = sectorX * sectorSize + localX;
        int y = sectorY * sectorSize + localY;

        Block floor = Blocks.stone;
        Block wall = Blocks.air;

        double elevation = sim.octaveNoise2D(detailed ? 7 : 2, 0.5, 1f / 500, x, y) * 4.1 - 1;
        double temp = vn.noise(x, y, 1f/200f)/2f + sim3.octaveNoise2D(detailed ? 12 : 6, 0.6, 1f / 620f, x, y);

        double r = sim2.octaveNoise2D(1, 0.6, 1f / 70, x, y);
        double edgeDist = Math.max(sectorSize / 2, sectorSize / 2) - Math.max(Math.abs(x - sectorSize / 2), Math.abs(y - sectorSize / 2));
        //double dst = Vector2.dst((sectorX * sectorSize) + sectorSize/2f, (sectorY * sectorSize) + sectorSize/2f, x, y);
        double elevDip = 30;

        double border = 14;

        if(edgeDist < border){
        //    elevation += (border - edgeDist) / 6.0;
        }

        if(temp < 0.35){
            floor = Blocks.snow;
        }else if(temp < 0.45){
            floor = Blocks.stone;
        }else if(temp < 0.65){
            floor = Blocks.grass;
        }else if(temp < 0.8){
            floor = Blocks.sand;
        }else if(temp < 0.9){
            floor = Blocks.blackstone;
            elevation = 0f;
        }else{
            floor = Blocks.lava;
        }

        //if(dst < elevDip){
        //    elevation -= (elevDip - dst) / elevDip * 3.0;
        /*}else*/if(detailed && r > 0.9){
            floor = Blocks.water;
            elevation = 0;

            if(r > 0.94){
                floor = Blocks.deepwater;
            }
        }

        if(detailed && wall == Blocks.air && decoration.containsKey(floor) && random.chance(0.03)){
            wall = decoration.get(floor);
        }

        result.wall = wall;
        result.floor = floor;
        result.elevation = (byte) Math.max(elevation, 0);
        return result;
    }

    public class GenResult{
        public Block floor, wall;
        public byte elevation;
    }

    public class OreEntry{
        final float frequency;
        final Item item;
        final Simplex noise;
        final RidgedPerlin ridge;
        final int index;

        OreEntry(Item item, float frequency, int seed){
            this.frequency = frequency;
            this.item = item;
            this.noise = new Simplex(seed + oreIndex);
            this.ridge = new RidgedPerlin(seed + oreIndex, 2);
            this.index = oreIndex++;
        }
    }
}
