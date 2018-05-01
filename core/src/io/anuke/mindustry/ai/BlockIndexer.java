package io.anuke.mindustry.ai;

import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.game.EventType.TileChangeEvent;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.flags.BlockFlag;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.util.EnumSet;

import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.world;

/**Class used for indexing special target blocks for AI.
 * TODO maybe use Arrays instead of ObjectSets?*/
public class BlockIndexer {
    /**Maps teams to a map of flagged tiles by type.*/
    private ObjectMap<BlockFlag, ObjectSet<Tile>> enemyMap = new ObjectMap<>();
    /**Maps teams to a map of flagged tiles by type.*/
    private ObjectMap<BlockFlag, ObjectSet<Tile>> allyMap = new ObjectMap<>();
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
            for(int x = 0; x < world.width(); x ++){
                for (int y = 0; y < world.height(); y++) {
                    process(world.tile(x, y));
                }
            }
        });
    }

    public ObjectSet<Tile> getAllied(Team team, BlockFlag type){
        return (state.teams.get(team).ally ? allyMap : enemyMap).get(type, emptyArray);
    }

    public ObjectSet<Tile> getEnemy(Team team, BlockFlag type){
        return (!state.teams.get(team).ally ? allyMap : enemyMap).get(type, emptyArray);
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
        return state.teams.get(team).ally ? allyMap : enemyMap;
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
