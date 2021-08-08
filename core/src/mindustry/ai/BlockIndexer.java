package mindustry.ai;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import java.util.*;

import static mindustry.Vars.*;

/** Class used for indexing special target blocks for AI. */
public class BlockIndexer{
    /** Size of one quadrant. */
    private static final int quadrantSize = 20;
    private static final Rect rect = new Rect();
    private static boolean returnBool = false;

    private int quadWidth, quadHeight;

    /** Stores all ore quadrants on the map. Maps ID to qX to qY to a list of tiles with that ore. */
    private IntSeq[][][] ores;
    /** Stores all damaged tile entities by team. */
    private Seq<Building>[] damagedTiles = new Seq[Team.all.length];
    /** All ores available on this map. */
    private ObjectSet<Item> allOres = new ObjectSet<>();
    /** Stores teams that are present here as tiles. */
    private Seq<Team> activeTeams = new Seq<>(Team.class);
    /** Maps teams to a map of flagged tiles by flag. */
    private TileArray[][] flagMap = new TileArray[Team.all.length][BlockFlag.all.length];
    /** Counts whether a certain floor is present in the world upon load. */
    private boolean[] blocksPresent;

    /** Array used for returning and reusing. */
    private Seq<Tile> returnArray = new Seq<>();
    /** Array used for returning and reusing. */
    private Seq<Building> breturnArray = new Seq<>(Building.class);

    public BlockIndexer(){
        clearFlags();

        Events.on(TilePreChangeEvent.class, event -> {
            removeIndex(event.tile);
        });

        Events.on(TileChangeEvent.class, event -> {
            addIndex(event.tile);
        });

        Events.on(WorldLoadEvent.class, event -> {
            damagedTiles = new Seq[Team.all.length];
            flagMap = new TileArray[Team.all.length][BlockFlag.all.length];
            activeTeams = new Seq<>(Team.class);

            clearFlags();

            allOres.clear();
            ores = new IntSeq[content.items().size][][];
            quadWidth = Mathf.ceil(world.width() / (float)quadrantSize);
            quadHeight = Mathf.ceil(world.height() / (float)quadrantSize);
            blocksPresent = new boolean[content.blocks().size];

            for(Tile tile : world.tiles){
                process(tile);

                var drop = tile.drop();

                if(drop != null){
                    allOres.add(drop);

                    int qx = (tile.x / quadrantSize);
                    int qy = (tile.y / quadrantSize);

                    //add position of quadrant to list
                    if(tile.block() == Blocks.air){
                        if(ores[drop.id] == null){
                            ores[drop.id] = new IntSeq[quadWidth][quadHeight];
                        }
                        if(ores[drop.id][qx][qy] == null){
                            ores[drop.id][qx][qy] = new IntSeq(false, 16);
                        }
                        ores[drop.id][qx][qy].add(tile.pos());
                    }
                }
            }
        });
    }

    public void removeIndex(Tile tile){
        var team = tile.team();
        if(tile.build != null && tile.isCenter()){
            var build = tile.build;
            var flags = tile.block().flags;
            var data = team.data();

            if(flags.size() > 0){
                for(BlockFlag flag : flags){
                    getFlagged(team)[flag.ordinal()].remove(tile);
                }
            }

            //update the unit cap when building is remove
            data.unitCap -= tile.block().unitCapModifier;

            //unregister building from building quadtree
            if(data.buildings != null){
                data.buildings.remove(build);
            }

            //is no longer registered
            build.wasDamaged = false;

            //unregister damaged buildings
            if(build.damaged() && damagedTiles[team.id] != null){
                damagedTiles[team.id].remove(build);
            }
        }
    }

    public void addIndex(Tile tile){
        process(tile);

        var drop = tile.drop();
        if(drop != null){
            int qx = tile.x / quadrantSize;
            int qy = tile.y / quadrantSize;

            if(ores[drop.id] == null){
                ores[drop.id] = new IntSeq[quadWidth][quadHeight];
            }
            if(ores[drop.id][qx][qy] == null){
                ores[drop.id][qx][qy] = new IntSeq(false, 16);
            }

            int pos = tile.pos();
            var seq = ores[drop.id][qx][qy];

            //when the drop can be mined, record the ore position
            if(tile.block() == Blocks.air && !seq.contains(pos)){
                seq.add(pos);
            }else{
                //otherwise, it likely became blocked, remove it (even if it wasn't there)
                seq.removeValue(pos);
            }
        }

    }

    /** @return whether a certain block is anywhere on this map. */
    public boolean isBlockPresent(Block block){
        return blocksPresent != null && blocksPresent[block.id];
    }

    private void clearFlags(){
        for(int i = 0; i < flagMap.length; i++){
            for(int j = 0; j < BlockFlag.all.length; j++){
                flagMap[i][j] = new TileArray();
            }
        }
    }

    private TileArray[] getFlagged(Team team){
        return flagMap[team.id];
    }

    /** @return whether this item is present on this map. */
    public boolean hasOre(Item item){
        return allOres.contains(item);
    }

    /** Returns all damaged tiles by team. */
    public Seq<Building> getDamaged(Team team){
        if(damagedTiles[team.id] == null){
            return damagedTiles[team.id] = new Seq<>(false);
        }

        return damagedTiles[team.id];
    }

    /** Get all allied blocks with a flag. */
    public TileArray getAllied(Team team, BlockFlag type){
        return flagMap[team.id][type.ordinal()];
    }

    @Nullable
    public Tile findClosestFlag(float x, float y, Team team, BlockFlag flag){
        return Geometry.findClosest(x, y, getAllied(team, flag));
    }

    public boolean eachBlock(Teamc team, float range, Boolf<Building> pred, Cons<Building> cons){
        return eachBlock(team.team(), team.getX(), team.getY(), range, pred, cons);
    }

    public boolean eachBlock(@Nullable Team team, float wx, float wy, float range, Boolf<Building> pred, Cons<Building> cons){
        breturnArray.clear();

        if(team == null){
            allBuildings(wx, wy, range, b -> {
                if(pred.get(b)){
                    breturnArray.add(b);
                }
            });
        }else{
            var buildings = team.data().buildings;
            if(buildings == null) return false;
            buildings.intersect(wx - range, wy - range, range*2f, range*2f, b -> {
                if(b.within(wx, wy, range + b.hitSize() / 2f) && pred.get(b)){
                    breturnArray.add(b);
                }
            });
        }

        int size = breturnArray.size;
        var items = breturnArray.items;
        for(int i = 0; i < size; i++){
            cons.get(items[i]);
            items[i] = null;
        }
        breturnArray.size = 0;

        return size > 0;
    }

    /** Get all enemy blocks with a flag. */
    public Seq<Tile> getEnemy(Team team, BlockFlag type){
        returnArray.clear();
        Seq<TeamData> data = state.teams.present;
        //when team data is not initialized, scan through every team. this is terrible
        if(data.isEmpty()){
            for(Team enemy : Team.all){
                if(enemy == team) continue;
                TileArray set = getFlagged(enemy)[type.ordinal()];
                if(set != null){
                    for(Tile tile : set){
                        returnArray.add(tile);
                    }
                }
            }
        }else{
            for(int i = 0; i < data.size; i++){
                Team enemy = data.items[i].team;
                if(enemy == team) continue;
                TileArray set = getFlagged(enemy)[type.ordinal()];
                if(set != null){
                    for(Tile tile : set){
                        returnArray.add(tile);
                    }
                }
            }
        }

        return returnArray;
    }

    public void notifyBuildHealed(Building build){
        if(build.wasDamaged && !build.damaged() && damagedTiles[build.team.id] != null){
            damagedTiles[build.team.id].remove(build);
            build.wasDamaged = false;
        }
    }

    public void notifyBuildDamaged(Building build){
        if(build.wasDamaged || !build.damaged()) return;

        if(damagedTiles[build.team.id] == null){
            damagedTiles[build.team.id] = new Seq<>(false);
        }

        damagedTiles[build.team.id].add(build);
        build.wasDamaged = true;
    }

    public void allBuildings(float x, float y, float range, Cons<Building> cons){
        breturnArray.clear();
        for(int i = 0; i < activeTeams.size; i++){
            Team team = activeTeams.items[i];
            var buildings = team.data().buildings;
            if(buildings == null) continue;
            buildings.intersect(x - range, y - range, range*2f, range*2f, breturnArray);
        }

        var items = breturnArray.items;
        int size = breturnArray.size;
        for(int i = 0; i < size; i++){
            var b = items[i];
            if(b.within(x, y, range + b.hitSize()/2f)){
                cons.get(b);
            }
            items[i] = null;
        }
        breturnArray.size = 0;
    }

    public Building findEnemyTile(Team team, float x, float y, float range, Boolf<Building> pred){
        Building target = null;
        float targetDist = 0;

        for(int i = 0; i < activeTeams.size; i++){
            Team enemy = activeTeams.items[i];
            if(enemy == team || (enemy == Team.derelict && !state.rules.coreCapture)) continue;

            Building candidate = indexer.findTile(enemy, x, y, range, pred, true);
            if(candidate == null) continue;

            //if a block has the same priority, the closer one should be targeted
            float dist = candidate.dst(x, y) - candidate.hitSize() / 2f;
            if(target == null ||
            //if its closer and is at least equal priority
            (dist < targetDist && candidate.block.priority.ordinal() >= target.block.priority.ordinal()) ||
            // block has higher priority (so range doesnt matter)
            (candidate.block.priority.ordinal() > target.block.priority.ordinal())){
                target = candidate;
                targetDist = dist;
            }
        }

        return target;
    }

    public Building findTile(Team team, float x, float y, float range, Boolf<Building> pred){
        return findTile(team, x, y, range, pred, false);
    }

    public Building findTile(Team team, float x, float y, float range, Boolf<Building> pred, boolean usePriority){
        Building closest = null;
        float dst = 0;
        var buildings = team.data().buildings;
        if(buildings == null) return null;

        breturnArray.clear();
        buildings.intersect(rect.setCentered(x, y, range * 2f), breturnArray);

        for(int i = 0; i < breturnArray.size; i++){
            var next = breturnArray.items[i];

            if(!pred.get(next) || !next.block.targetable) continue;

            float bdst = next.dst(x, y) - next.hitSize() / 2f;
            if(bdst < range && (closest == null ||
            //this one is closer, and it is at least of equal priority
            (bdst < dst && (!usePriority || closest.block.priority.ordinal() <= next.block.priority.ordinal())) ||
            //priority is used, and new block has higher priority regardless of range
            (usePriority && closest.block.priority.ordinal() < next.block.priority.ordinal()))){
                dst = bdst;
                closest = next;
            }
        }

        return closest;
    }

    /** Find the closest ore block relative to a position. */
    public Tile findClosestOre(float xp, float yp, Item item){
        if(ores[item.id] != null){
            float minDst = 0f;
            Tile closest = null;
            for(int qx = 0; qx < quadWidth; qx++){
                for(int qy = 0; qy < quadHeight; qy++){
                    var arr = ores[item.id][qx][qy];
                    if(arr != null && arr.size > 0){
                        Tile tile = world.tile(arr.first());
                        float dst = Mathf.dst2(xp, yp, tile.worldx(), tile.worldy());
                        if(closest == null || dst < minDst){
                            closest = tile;
                            minDst = dst;
                        }
                    }
                }
            }
            return closest;
        }

        return null;
    }

    /** Find the closest ore block relative to a position. */
    public Tile findClosestOre(Unit unit, Item item){
        return findClosestOre(unit.x, unit.y, item);
    }

    private void process(Tile tile){
        var team = tile.team();
        //only process entity changes with centered tiles
        if(tile.isCenter() && tile.build != null){
            var data = team.data();
            if(tile.block().flags.size() > 0 && tile.isCenter()){
                TileArray[] map = getFlagged(team);

                for(BlockFlag flag : tile.block().flags){

                    TileArray arr = map[flag.ordinal()];

                    arr.add(tile);

                    map[flag.ordinal()] = arr;
                }
            }

            //update the unit cap when new tile is registered
            data.unitCap += tile.block().unitCapModifier;

            if(!activeTeams.contains(team)){
                activeTeams.add(team);
            }

            //insert the new tile into the quadtree for targeting
            if(data.buildings == null){
                data.buildings = new QuadTree<>(new Rect(0, 0, world.unitWidth(), world.unitHeight()));
            }
            data.buildings.insert(tile.build);

            notifyBuildDamaged(tile.build);
        }

        if(!tile.block().isStatic()){
            blocksPresent[tile.floorID()] = true;
            blocksPresent[tile.overlayID()] = true;
        }
        //bounds checks only needed in very specific scenarios
        if(tile.blockID() < blocksPresent.length) blocksPresent[tile.blockID()] = true;
    }

    public static class TileArray implements Iterable<Tile>{
        Seq<Tile> tiles = new Seq<>(false, 16);
        IntSet contained = new IntSet();

        public void add(Tile tile){
            if(contained.add(tile.pos())){
                tiles.add(tile);
            }
        }

        public void remove(Tile tile){
            if(contained.remove(tile.pos())){
                tiles.remove(tile);
            }
        }

        public int size(){
            return tiles.size;
        }

        public Tile first(){
            return tiles.first();
        }

        @Override
        public Iterator<Tile> iterator(){
            return tiles.iterator();
        }
    }
}
