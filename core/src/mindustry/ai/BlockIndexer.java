package mindustry.ai;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.Units.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/** Class used for indexing special target blocks for AI. */
public class BlockIndexer{
    /** Size of one quadrant. */
    private static final int quadrantSize = 20;
    private static final Rect rect = new Rect();
    private static boolean returnBool = false;

    private int quadWidth, quadHeight;

    /** Stores all ore quadrants on the map. Maps ID to qX to qY to a list of tiles with that ore. */
    private IntSeq[][][] ores, wallOres;
    /** Stores all damaged tile entities by team. */
    private Seq<Building>[] damagedTiles = new Seq[Team.all.length];
    /** All ores present on the map - can be wall or floor. */
    private Seq<Item> allPresentOres = new Seq<>();
    /** All ores available on this map. */
    private ObjectIntMap<Item> allOres = new ObjectIntMap<>(), allWallOres = new ObjectIntMap<>();
    /** Stores teams that are present here as tiles. */
    private Seq<Team> activeTeams = new Seq<>(Team.class);
    /** Maps teams to a map of flagged tiles by flag. */
    private Seq<Building>[][] flagMap = new Seq[Team.all.length][BlockFlag.all.length];
    /** Counts whether a certain floor is present in the world upon load. */
    private boolean[] blocksPresent;
    /** Array used for returning and reusing. */
    private Seq<Building> breturnArray = new Seq<>(Building.class);
    /** Maps block flag to a list of floor tiles that have it. */
    private Seq<Tile>[] floorMap;

    public BlockIndexer(){
        clearFlags();

        Events.on(TilePreChangeEvent.class, event -> {
            removeIndex(event.tile);
        });

        Events.on(TileChangeEvent.class, event -> {
            addIndex(event.tile);
        });

        Events.on(TileFloorChangeEvent.class, event -> {
            removeFloorIndex(event.tile, event.previous);
            addFloorIndex(event.tile, event.floor);
        });

        Events.on(WorldLoadEvent.class, event -> {
            damagedTiles = new Seq[Team.all.length];
            flagMap = new Seq[Team.all.length][BlockFlag.all.length];
            floorMap = new Seq[BlockFlag.all.length];
            activeTeams = new Seq<>(Team.class);

            clearFlags();

            allOres.clear();
            allWallOres.clear();
            ores = new IntSeq[content.items().size][][];
            wallOres = new IntSeq[content.items().size][][];
            quadWidth = Mathf.ceil(world.width() / (float)quadrantSize);
            quadHeight = Mathf.ceil(world.height() / (float)quadrantSize);
            blocksPresent = new boolean[content.blocks().size];

            //so WorldLoadEvent gets called twice sometimes... ugh
            for(Team team : Team.all){
                var data = state.teams.get(team);
                if(data != null){
                    if(data.buildingTree != null) data.buildingTree.clear();
                    if(data.turretTree != null) data.turretTree.clear();
                }
            }

            for(Tile tile : world.tiles){
                process(tile);

                addFloorIndex(tile, tile.floor());

                Item drop;
                int qx = tile.x / quadrantSize, qy = tile.y / quadrantSize;
                if(tile.block() == Blocks.air){
                    if((drop = tile.drop()) != null){
                        //add position of quadrant to list
                        if(ores[drop.id] == null) ores[drop.id] = new IntSeq[quadWidth][quadHeight];
                        if(ores[drop.id][qx][qy] == null) ores[drop.id][qx][qy] = new IntSeq(false, 16);
                        ores[drop.id][qx][qy].add(tile.pos());
                        allOres.increment(drop);
                    }
                }else if((drop = tile.wallDrop()) != null){
                    //add position of quadrant to list
                    if(wallOres[drop.id] == null) wallOres[drop.id] = new IntSeq[quadWidth][quadHeight];
                    if(wallOres[drop.id][qx][qy] == null) wallOres[drop.id][qx][qy] = new IntSeq(false, 16);
                    wallOres[drop.id][qx][qy].add(tile.pos());
                    allWallOres.increment(drop);
                }
            }

            updatePresentOres();

            for(Team team : Team.all){
                var data = state.teams.get(team);

                if(team.rules().prebuildAi && data.hasCore()){
                    PrebuildAI.sortPlans(data.plans);
                }
            }
        });
    }

    public Seq<Item> getAllPresentOres(){
        return allPresentOres;
    }

    private void updatePresentOres(){
        allPresentOres.clear();
        for(Item item : content.items()){
            if(hasOre(item) || hasWallOre(item)){
                allPresentOres.add(item);
            }
        }
    }

    private void removeFloorIndex(Tile tile, Floor floor){
        if(floor.flags.size == 0 || floorMap == null) return;

        for(var flag : floor.flags.array){
            getFlaggedFloors(flag).remove(tile);
        }
    }

    private void addFloorIndex(Tile tile, Floor floor){
        if(floor.flags.size == 0 || !floor.shouldIndex(tile) || floorMap == null) return;

        for(var flag : floor.flags.array){
            getFlaggedFloors(flag).add(tile);
        }
    }

    public Seq<Tile> getFlaggedFloors(BlockFlag flag){
        if(floorMap[flag.ordinal()] == null){
            floorMap[flag.ordinal()] = new Seq<>(false);
        }
        return floorMap[flag.ordinal()];
    }

    public void removeIndex(Tile tile){
        var team = tile.team();
        if(tile.build != null && tile.isCenter()){
            var build = tile.build;
            var flags = tile.block().flags;
            var data = team.data();

            if(flags.size > 0){
                for(BlockFlag flag : flags.array){
                    getFlagged(team)[flag.ordinal()].remove(build);
                }
            }

            //no longer part of the building list
            data.buildings.remove(build);
            data.buildingTypes.get(build.block, () -> new Seq<>(false)).remove(build);

            //update the unit cap when building is removed
            data.unitCap -= tile.block().unitCapModifier;

            //unregister building from building quadtree
            if(data.buildingTree != null){
                data.buildingTree.remove(build);
            }

            //remove indexed turret
            if(data.turretTree != null && build.block.attacks){
                data.turretTree.remove(build);
            }

            //unregister damaged buildings
            if(build.wasDamaged && damagedTiles[team.id] != null){
                damagedTiles[team.id].remove(build);
            }

            //is no longer registered
            build.wasDamaged = false;
        }
    }

    public void addIndex(Tile base){
        process(base);

        base.getLinkedTiles(tile -> {
            Item drop = tile.drop(), wallDrop = tile.wallDrop();
            if(drop == null && wallDrop == null) return;
            int qx = tile.x / quadrantSize, qy = tile.y / quadrantSize;
            int pos = tile.pos();

            if(tile.block() == Blocks.air){
                if(drop != null){ //floor
                    if(ores[drop.id] == null) ores[drop.id] = new IntSeq[quadWidth][quadHeight];
                    if(ores[drop.id][qx][qy] == null) ores[drop.id][qx][qy] = new IntSeq(false, 16);
                    if(ores[drop.id][qx][qy].addUnique(pos)){
                        int old = allOres.increment(drop); //increment ore count only if not already counted
                        if(old == 0) updatePresentOres();
                    }
                }
                if(wallDrop != null && wallOres != null && wallOres[wallDrop.id] != null && wallOres[wallDrop.id][qx][qy] != null && wallOres[wallDrop.id][qx][qy].removeValue(pos)){ //wall
                    int old = allWallOres.increment(wallDrop, -1);
                    if(old == 1) updatePresentOres();
                }
            }else{
                if(wallDrop != null){ //wall
                    if(wallOres[wallDrop.id] == null) wallOres[wallDrop.id] = new IntSeq[quadWidth][quadHeight];
                    if(wallOres[wallDrop.id][qx][qy] == null) wallOres[wallDrop.id][qx][qy] = new IntSeq(false, 16);
                    if(wallOres[wallDrop.id][qx][qy].addUnique(pos)){
                        int old = allWallOres.increment(wallDrop); //increment ore count only if not already counted
                        if(old == 0) updatePresentOres();
                    }
                }

                if(drop != null && ores != null && ores[drop.id] != null && ores[drop.id][qx][qy] != null && ores[drop.id][qx][qy].removeValue(pos)){ //floor
                    int old = allOres.increment(drop, -1);
                    if(old == 1) updatePresentOres();
                }
            }
        });
    }

    /** @return whether a certain block is anywhere on this map. */
    public boolean isBlockPresent(Block block){
        return blocksPresent != null && blocksPresent[block.id];
    }

    private void clearFlags(){
        for(int i = 0; i < flagMap.length; i++){
            for(int j = 0; j < BlockFlag.all.length; j++){
                flagMap[i][j] = new Seq();
            }
        }
    }

    private Seq<Building>[] getFlagged(Team team){
        return flagMap[team.id];
    }

    /** @return whether this item is present on this map. */
    public boolean hasOre(Item item){
        return allOres.get(item) > 0;
    }

    /** @return whether this item is present on this map as a wall ore. */
    public boolean hasWallOre(Item item){
        return allWallOres.get(item) > 0;
    }

    /** Returns all damaged tiles by team. */
    public Seq<Building> getDamaged(Team team){
        if(damagedTiles[team.id] == null){
            return damagedTiles[team.id] = new Seq<>(false);
        }

        var tiles = damagedTiles[team.id];
        tiles.removeAll(b -> !b.damaged());

        return tiles;
    }

    /** Get all allied blocks with a flag. */
    public Seq<Building> getFlagged(Team team, BlockFlag type){
        return flagMap[team.id][type.ordinal()];
    }

    @Nullable
    public Building findClosestFlag(float x, float y, Team team, BlockFlag flag){
        return Geometry.findClosest(x, y, getFlagged(team, flag));
    }

    public boolean eachBlock(Teamc team, float range, Boolf<Building> pred, Cons<Building> cons){
        return eachBlock(team.team(), team.getX(), team.getY(), range, pred, cons);
    }

    public boolean eachBlock(@Nullable Team team, float wx, float wy, float range, Boolf<Building> pred, Cons<Building> cons){

        if(team == null){
            returnBool = false;

            allBuildings(wx, wy, range, b -> {
                if(pred.get(b)){
                    returnBool = true;
                    cons.get(b);
                }
            });
            return returnBool;
        }else{
            breturnArray.clear();

            var buildings = team.data().buildingTree;
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

    /** Does not work with null teams. */
    public boolean eachBlock(Team team, Rect rect, Boolf<Building> pred, Cons<Building> cons){
        if(team == null) return false;

        breturnArray.clear();

        var buildings = team.data().buildingTree;
        if(buildings == null) return false;
        buildings.intersect(rect, b -> {
            if(pred.get(b)){
                breturnArray.add(b);
            }
        });

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
    public Seq<Building> getEnemy(Team team, BlockFlag type){
        breturnArray.clear();
        Seq<TeamData> data = state.teams.present;
        //when team data is not initialized, scan through every team. this is terrible
        if(data.isEmpty()){
            for(Team enemy : Team.all){
                if(enemy == team || (enemy == Team.derelict && !state.rules.coreCapture)) continue;
                var set = getFlagged(enemy)[type.ordinal()];
                if(set != null){
                    breturnArray.addAll(set);
                }
            }
        }else{
            for(int i = 0; i < data.size; i++){
                Team enemy = data.items[i].team;
                if(enemy == team || (enemy == Team.derelict && !state.rules.coreCapture)) continue;
                var set = getFlagged(enemy)[type.ordinal()];
                if(set != null){
                    breturnArray.addAll(set);
                }
            }
        }

        return breturnArray;
    }

    public void notifyHealthChanged(Building build){
        boolean damaged = build.damaged();

        if(build.wasDamaged != damaged){
            if(damagedTiles[build.team.id] == null){
                damagedTiles[build.team.id] = new Seq<>(false);
            }

            if(damaged){
                //is now damaged, add to array
                damagedTiles[build.team.id].add(build);
            }else{
                //no longer damaged, remove
                damagedTiles[build.team.id].remove(build);
            }

            build.wasDamaged = damaged;
        }
    }

    public void allBuildings(float x, float y, float range, Cons<Building> cons){
        breturnArray.clear();
        for(int i = 0; i < activeTeams.size; i++){
            Team team = activeTeams.items[i];
            var buildings = team.data().buildingTree;
            if(buildings == null) continue;
            buildings.intersect(x - range, y - range, range*2f, range*2f, breturnArray);
        }

        var items = breturnArray.items;
        int size = breturnArray.size;
        for(int i = 0; i < size; i++){
            var b = items[i];
            if(b != null && b.within(x, y, range + b.hitSize()/2f)){
                cons.get(b);
            }
            items[i] = null;
        }
        breturnArray.size = 0;
    }

    public Building findEnemyTile(Team team, float x, float y, float range, BuildingPriorityf priority, Boolf<Building> pred){
        Building target = null;
        float targetDist = 0;

        for(int i = 0; i < activeTeams.size; i++){
            Team enemy = activeTeams.items[i];
            if(enemy == team || (enemy == Team.derelict && !state.rules.coreCapture)) continue;

            Building candidate = indexer.findTile(enemy, x, y, range, b -> pred.get(b) && b.isDiscovered(team), true);
            if(candidate == null) continue;

            //if a block has the same priority, the closer one should be targeted
            float dist = candidate.dst(x, y) - candidate.hitSize() / 2f;
            if(target == null ||
            //if it is closer and is at least equal priority
            (dist < targetDist && priority.priority(candidate) >= priority.priority(target)) ||
            // block has higher priority (so range doesn't matter)
            priority.priority(candidate) > priority.priority(target)){
                target = candidate;
                targetDist = dist;
            }
        }

        return target;
    }

    public Building findEnemyTile(Team team, float x, float y, float range, Boolf<Building> pred){
        return findEnemyTile(team, x, y, range, UnitSorts.buildingDefault, pred);
    }

    public Building findTile(Team team, float x, float y, float range, Boolf<Building> pred){
        return findTile(team, x, y, range, pred, false);
    }

    public Building findTile(Team team, float x, float y, float range, Boolf<Building> pred, boolean usePriority){
        Building closest = null;
        float dst = 0;
        var buildings = team.data().buildingTree;
        if(buildings == null) return null;

        breturnArray.clear();
        buildings.intersect(rect.setCentered(x, y, range * 2f), breturnArray);

        for(int i = 0; i < breturnArray.size; i++){
            var next = breturnArray.items[i];

            if(!pred.get(next) || !next.block.targetable) continue;

            float bdst = next.dst(x, y) - next.hitSize() / 2f;
            if(bdst < range && (closest == null ||
            //this one is closer, and it is at least of equal priority
            (bdst < dst && (!usePriority || closest.block.priority <= next.block.priority)) ||
            //priority is used, and new block has higher priority regardless of range
            (usePriority && closest.block.priority < next.block.priority))){
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
                        if(tile.block() == Blocks.air){
                            float dst = Mathf.dst2(xp, yp, tile.worldx(), tile.worldy());
                            if(closest == null || dst < minDst){
                                closest = tile;
                                minDst = dst;
                            }
                        }
                    }
                }
            }
            return closest;
        }

        return null;
    }

    /** Find the closest ore wall relative to a position. */
    public Tile findClosestWallOre(float xp, float yp, Item item){
        //(stolen from foo's client :))))
        if(wallOres[item.id] != null){
            float minDst = 0f;
            Tile closest = null;
            for(int qx = 0; qx < quadWidth; qx++){
                for(int qy = 0; qy < quadHeight; qy++){
                    var arr = wallOres[item.id][qx][qy];
                    if(arr != null && arr.size > 0){
                        Tile tile = world.tile(arr.first());
                        if(tile.block() != Blocks.air){
                            float dst = Mathf.dst2(xp, yp, tile.worldx(), tile.worldy());
                            if(closest == null || dst < minDst){
                                closest = tile;
                                minDst = dst;
                            }
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

    /** Find the closest ore block relative to a position. */
    public Tile findClosestWallOre(Unit unit, Item item){
        return findClosestWallOre(unit.x, unit.y, item);
    }

    private void process(Tile tile){
        var team = tile.team();
        //only process entity changes with centered tiles
        if(tile.isCenter() && tile.build != null){
            var data = team.data();

            if(tile.block().flags.size > 0 && tile.isCenter()){
                var map = getFlagged(team);

                for(BlockFlag flag : tile.block().flags.array){
                    map[flag.ordinal()].add(tile.build);
                }
            }

            //record in list of buildings
            data.buildings.add(tile.build);
            data.buildingTypes.get(tile.block(), () -> new Seq<>(false)).add(tile.build);

            //update the unit cap when new tile is registered
            data.unitCap += tile.block().unitCapModifier;

            if(!activeTeams.contains(team)){
                activeTeams.add(team);
            }

            //insert the new tile into the quadtree for targeting
            if(data.buildingTree == null){
                data.buildingTree = new QuadTree<>(new Rect(0, 0, world.unitWidth(), world.unitHeight()));
            }
            data.buildingTree.insert(tile.build);

            if(tile.block().attacks && tile.build instanceof Ranged){
                if(data.turretTree == null){
                    data.turretTree = new TurretQuadtree(new Rect(0, 0, world.unitWidth(), world.unitHeight()));
                }

                data.turretTree.insert(tile.build);
            }

            notifyHealthChanged(tile.build);
        }

        if(blocksPresent != null){
            if(!tile.block().isStatic()){
                blocksPresent[tile.floorID()] = true;
                blocksPresent[tile.overlayID()] = true;
            }
            //bounds checks only needed in very specific scenarios
            if(tile.blockID() < blocksPresent.length) blocksPresent[tile.blockID()] = true;
        }

    }

    static class TurretQuadtree extends QuadTree<Building>{

        public TurretQuadtree(Rect bounds){
            super(bounds);
        }

        @Override
        public void hitbox(Building build){
            tmp.setCentered(build.x, build.y, ((Ranged)build).range() * 2f);
        }

        @Override
        protected QuadTree<Building> newChild(Rect rect){
            return new TurretQuadtree(rect);
        }
    }
}
