package io.anuke.mindustry.ai;

import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.game.EventType.TileChangeEvent;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.BlockFlag;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.util.EnumSet;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.world;

//TODO consider using quadtrees for finding specific types of blocks within an area
/**Class used for indexing special target blocks for AI.
 * TODO maybe use Arrays instead of ObjectSets?*/
public class BlockIndexer {
    /**Size of one ore quadrant.*/
    private final static int quadrantSize = 12;
    /**Set of all ores that are being scanned.*/
    private final ObjectSet<Item> scanOres = ObjectSet.with(Items.iron, Items.coal, Items.lead, Items.thorium, Items.titanium);
    /**Stores all ore quadtrants on the map.*/
    private ObjectMap<Item, ObjectSet<Tile>> ores = new ObjectMap<>();

    /**Maps teams to a map of flagged tiles by type.*/
    private ObjectMap<BlockFlag, ObjectSet<Tile>> enemyMap = new ObjectMap<>();
    /**Maps teams to a map of flagged tiles by type.*/
    private ObjectMap<BlockFlag, ObjectSet<Tile>> allyMap = new ObjectMap<>();
    /**Empty map for invalid teams.*/
    private ObjectMap<BlockFlag, ObjectSet<Tile>> emptyMap = new ObjectMap<>();
    /**Maps tile positions to their last known tile index data.*/
    private IntMap<TileIndex> typeMap = new IntMap<>();
    /**Empty array used for returning.*/
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
        });

        Events.on(WorldLoadEvent.class, () -> {
            enemyMap.clear();
            allyMap.clear();
            typeMap.clear();
            ores.clear();
            for(int x = 0; x < world.width(); x ++){
                for (int y = 0; y < world.height(); y++) {
                    process(world.tile(x, y));
                }
            }

            scanOres();
        });
    }

    /**Get all allied blocks with a flag.*/
    public ObjectSet<Tile> getAllied(Team team, BlockFlag type){
        return (state.teams.get(team).ally ? allyMap : enemyMap).get(type, emptyArray);
    }

    /**Get all enemy blocks with a flag.*/
    public ObjectSet<Tile> getEnemy(Team team, BlockFlag type){
        return (!state.teams.get(team).ally ? allyMap : enemyMap).get(type, emptyArray);
    }

    /**Returns a set of tiles that have ores of the specified type nearby.
     * While each tile in the set is not guaranteed to have an ore directly on it,
     * each tile will at least have an ore within {@link #quadrantSize} / 2 blocks of it.
     * Only specific ore types are scanned. See {@link #scanOres}.*/
    public ObjectSet<Tile> getOrePositions(Item item){
        return ores.get(item, emptyArray);
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
    }

    private ObjectMap<BlockFlag, ObjectSet<Tile>> getMap(Team team){
        if(!state.teams.has(team)) return emptyMap;
        return state.teams.get(team).ally ? allyMap : enemyMap;
    }

    private void scanOres(){
        //initialize ore map with empty sets
        for(Item item : scanOres){
            ores.put(item, new ObjectSet<>());
        }

        for(int x = 0; x < world.width(); x ++){
            for (int y = 0; y < world.height(); y++) {
                int qx = (x/quadrantSize);
                int qy = (y/quadrantSize);

                Tile tile = world.tile(x, y);

                //add position of quadrant to list when an ore is found
                if(tile.floor().drops != null && scanOres.contains(tile.floor().drops.item)){
                    ores.get(tile.floor().drops.item).add(world.tile(
                            //make sure to clamp quadrant middle position, since it might go off bounds
                            Mathf.clamp(qx * quadrantSize + quadrantSize/2, 0, world.width() - 1),
                            Mathf.clamp(qy * quadrantSize + quadrantSize/2, 0, world.height() - 1)));
                }
            }
        }
    }

    private class TileIndex{
        public final EnumSet<BlockFlag> flags;
        public final Team team;

        public TileIndex(EnumSet<BlockFlag> flags, Team team) {
            this.flags = flags;
            this.team = team;
        }
    }
}
