package io.anuke.mindustry.ai;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Bits;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.game.EventType.TileChangeEvent;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.TeamInfo.TeamData;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockFlag;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.entities.trait.Entity;
import io.anuke.ucore.function.Predicate;
import io.anuke.ucore.util.EnumSet;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

//TODO consider using quadtrees for finding specific types of blocks within an area
//TODO maybe use Arrays instead of ObjectSets?

/**
 * Class used for indexing special target blocks for AI.
 */
public class BlockIndexer{
    /**
     * Size of one ore quadrant.
     */
    private final static int oreQuadrantSize = 20;
    /**
     * Size of one structure quadrant.
     */
    private final static int structQuadrantSize = 12;

    /**
     * Set of all ores that are being scanned.
     */
    private final ObjectSet<Item> scanOres = ObjectSet.with(Items.tungsten, Items.coal, Items.lead, Items.thorium, Items.titanium);
    private final ObjectSet<Item> itemSet = new ObjectSet<>();
    /**
     * Stores all ore quadtrants on the map.
     */
    private ObjectMap<Item, ObjectSet<Tile>> ores;
    /**
     * Tags all quadrants.
     */
    private Bits[] structQuadrants;

    /**
     * Maps teams to a map of flagged tiles by type.
     */
    private ObjectMap<BlockFlag, ObjectSet<Tile>> enemyMap = new ObjectMap<>();
    /**
     * Maps teams to a map of flagged tiles by type.
     */
    private ObjectMap<BlockFlag, ObjectSet<Tile>> allyMap = new ObjectMap<>();
    /**
     * Empty map for invalid teams.
     */
    private ObjectMap<BlockFlag, ObjectSet<Tile>> emptyMap = new ObjectMap<>();
    /**
     * Maps tile positions to their last known tile index data.
     */
    private IntMap<TileIndex> typeMap = new IntMap<>();
    /**
     * Empty array used for returning.
     */
    private ObjectSet<Tile> emptyArray = new ObjectSet<>();

    public BlockIndexer(){
        Events.on(TileChangeEvent.class, tile -> {
            if(typeMap.get(tile.packedPosition()) != null){
                TileIndex index = typeMap.get(tile.packedPosition());
                for(BlockFlag flag : index.flags){
                    getMap(index.team).get(flag).remove(tile);
                }
            }
            process(tile);
            updateQuadrant(tile);
        });

        Events.on(WorldLoadEvent.class, () -> {
            enemyMap.clear();
            allyMap.clear();
            typeMap.clear();
            ores = null;

            //create bitset for each team type that contains each quadrant
            structQuadrants = new Bits[Team.all.length];
            for(int i = 0; i < Team.all.length; i++){
                structQuadrants[i] = new Bits(Mathf.ceil(world.width() / (float) structQuadrantSize) * Mathf.ceil(world.height() / (float) structQuadrantSize));
            }

            for(int x = 0; x < world.width(); x++){
                for(int y = 0; y < world.height(); y++){
                    process(world.tile(x, y));
                }
            }

            for(int x = 0; x < quadWidth(); x++){
                for(int y = 0; y < quadHeight(); y++){
                    updateQuadrant(world.tile(x * structQuadrantSize, y * structQuadrantSize));
                }
            }

            scanOres();
        });
    }

    /**
     * Get all allied blocks with a flag.
     */
    public ObjectSet<Tile> getAllied(Team team, BlockFlag type){
        return (state.teams.get(team).ally ? allyMap : enemyMap).get(type, emptyArray);
    }

    /**
     * Get all enemy blocks with a flag.
     */
    public ObjectSet<Tile> getEnemy(Team team, BlockFlag type){
        return (!state.teams.get(team).ally ? allyMap : enemyMap).get(type, emptyArray);
    }

    public TileEntity findTile(Team team, float x, float y, float range, Predicate<Tile> pred){
        Entity closest = null;
        float dst = 0;

        for(int rx = Math.max((int) ((x - range) / tilesize / structQuadrantSize), 0); rx <= (int) ((x + range) / tilesize / structQuadrantSize) && rx < quadWidth(); rx++){
            for(int ry = Math.max((int) ((y - range) / tilesize / structQuadrantSize), 0); ry <= (int) ((y + range) / tilesize / structQuadrantSize) && ry < quadHeight(); ry++){

                if(!getQuad(team, rx, ry)) continue;

                for(int tx = rx * structQuadrantSize; tx < (rx + 1) * structQuadrantSize && tx < world.width(); tx++){
                    for(int ty = ry * structQuadrantSize; ty < (ry + 1) * structQuadrantSize && ty < world.height(); ty++){
                        Tile other = world.tile(tx, ty);

                        if(other == null || other.entity == null || !pred.test(other)) continue;

                        TileEntity e = other.entity;

                        float ndst = Vector2.dst(x, y, e.x, e.y);
                        if(ndst < range && (closest == null || ndst < dst)){
                            dst = ndst;
                            closest = e;
                        }
                    }
                }
            }
        }

        return (TileEntity) closest;
    }

    /**
     * Returns a set of tiles that have ores of the specified type nearby.
     * While each tile in the set is not guaranteed to have an ore directly on it,
     * each tile will at least have an ore within {@link #oreQuadrantSize} / 2 blocks of it.
     * Only specific ore types are scanned. See {@link #scanOres}.
     */
    public ObjectSet<Tile> getOrePositions(Item item){
        return ores.get(item, emptyArray);
    }

    /**
     * Find the closest ore block relative to a position.
     */
    public Tile findClosestOre(float xp, float yp, Item item){
        Tile tile = Geometry.findClosest(xp, yp, world.indexer().getOrePositions(item));

        if(tile == null) return null;

        for(int x = Math.max(0, tile.x - oreQuadrantSize / 2); x < tile.x + oreQuadrantSize / 2 && x < world.width(); x++){
            for(int y = Math.max(0, tile.y - oreQuadrantSize / 2); y < tile.y + oreQuadrantSize / 2 && y < world.height(); y++){
                Tile res = world.tile(x, y);
                if(res.block() == Blocks.air && res.floor().drops != null && res.floor().drops.item == item){
                    return res;
                }
            }
        }

        return null;
    }

    private void process(Tile tile){
        if(tile.block().flags != null &&
                tile.getTeam() != Team.none){
            ObjectMap<BlockFlag, ObjectSet<Tile>> map = getMap(tile.getTeam());

            for(BlockFlag flag : tile.block().flags){

                ObjectSet<Tile> arr = map.get(flag);
                if(arr == null){
                    arr = new ObjectSet<>();
                    map.put(flag, arr);
                }

                arr.add(tile);

                map.put(flag, arr);
            }
            typeMap.put(tile.packedPosition(), new TileIndex(tile.block().flags, tile.getTeam()));
        }

        if(ores == null) return;

        int quadrantX = tile.x / oreQuadrantSize;
        int quadrantY = tile.y / oreQuadrantSize;
        itemSet.clear();

        Tile rounded = world.tile(Mathf.clamp(quadrantX * oreQuadrantSize + oreQuadrantSize / 2, 0, world.width() - 1),
                Mathf.clamp(quadrantY * oreQuadrantSize + oreQuadrantSize / 2, 0, world.height() - 1));

        //find all items that this quadrant contains
        for(int x = quadrantX * structQuadrantSize; x < world.width() && x < (quadrantX + 1) * structQuadrantSize; x++){
            for(int y = quadrantY * structQuadrantSize; y < world.height() && y < (quadrantY + 1) * structQuadrantSize; y++){
                Tile result = world.tile(x, y);
                if(result.block().drops == null || !scanOres.contains(result.block().drops.item)) continue;

                itemSet.add(result.block().drops.item);
            }
        }

        //update quadrant at this position
        for(Item item : scanOres){
            ObjectSet<Tile> set = ores.get(item);

            //update quadrant status depending on whether the item is in it
            if(!itemSet.contains(item)){
                set.remove(rounded);
            }else{
                set.add(rounded);
            }
        }
    }

    private void updateQuadrant(Tile tile){
        //this quadrant is now 'dirty', re-scan the whole thing
        int quadrantX = tile.x / structQuadrantSize;
        int quadrantY = tile.y / structQuadrantSize;
        int index = quadrantX + quadrantY * quadWidth();
        //Log.info("Updating quadrant: {0} {1}", quadrantX, quadrantY);

        for(TeamData data : state.teams.getTeams()){

            //fast-set this quadrant to 'occupied' if the tile just placed is already of this team
            if(tile.getTeam() == data.team && tile.entity != null){
                structQuadrants[data.team.ordinal()].set(index);
                continue; //no need to process futher
            }

            structQuadrants[data.team.ordinal()].clear(index);

            outer:
            for(int x = quadrantX * structQuadrantSize; x < world.width() && x < (quadrantX + 1) * structQuadrantSize; x++){
                for(int y = quadrantY * structQuadrantSize; y < world.height() && y < (quadrantY + 1) * structQuadrantSize; y++){
                    Tile result = world.tile(x, y);
                    //when a targetable block is found, mark this quadrant as occupied and stop searching
                    if(result.entity != null && result.getTeam() == data.team){
                        structQuadrants[data.team.ordinal()].set(index);
                        break outer;
                    }
                }
            }
        }
    }

    private boolean getQuad(Team team, int quadrantX, int quadrantY){
        int index = quadrantX + quadrantY * Mathf.ceil(world.width() / (float) structQuadrantSize);
        return structQuadrants[team.ordinal()].get(index);
    }

    private int quadWidth(){
        return Mathf.ceil(world.width() / (float) structQuadrantSize);
    }

    private int quadHeight(){
        return Mathf.ceil(world.height() / (float) structQuadrantSize);
    }

    private ObjectMap<BlockFlag, ObjectSet<Tile>> getMap(Team team){
        if(!state.teams.has(team)) return emptyMap;
        return state.teams.get(team).ally ? allyMap : enemyMap;
    }

    private void scanOres(){
        ores = new ObjectMap<>();

        //initialize ore map with empty sets
        for(Item item : scanOres){
            ores.put(item, new ObjectSet<>());
        }

        for(int x = 0; x < world.width(); x++){
            for(int y = 0; y < world.height(); y++){
                int qx = (x / oreQuadrantSize);
                int qy = (y / oreQuadrantSize);

                Tile tile = world.tile(x, y);

                //add position of quadrant to list when an ore is found
                if(tile.floor().drops != null && scanOres.contains(tile.floor().drops.item) && tile.block() == Blocks.air){
                    ores.get(tile.floor().drops.item).add(world.tile(
                            //make sure to clamp quadrant middle position, since it might go off bounds
                            Mathf.clamp(qx * oreQuadrantSize + oreQuadrantSize / 2, 0, world.width() - 1),
                            Mathf.clamp(qy * oreQuadrantSize + oreQuadrantSize / 2, 0, world.height() - 1)));
                }
            }
        }
    }

    private class TileIndex{
        public final EnumSet<BlockFlag> flags;
        public final Team team;

        public TileIndex(EnumSet<BlockFlag> flags, Team team){
            this.flags = flags;
            this.team = team;
        }
    }
}
