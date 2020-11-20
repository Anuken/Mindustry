package mindustry.ai;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.EnumSet;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

import java.util.*;

import static mindustry.Vars.*;

/** Class used for indexing special target blocks for AI. */
public class BlockIndexer{
    /** Size of one quadrant. */
    private static final int quadrantSize = 16;

    /** Set of all ores that are being scanned. */
    private final ObjectSet<Item> scanOres = new ObjectSet<>();
    private final IntSet intSet = new IntSet();
    private final ObjectSet<Item> itemSet = new ObjectSet<>();
    /** Stores all ore quadtrants on the map. */
    private ObjectMap<Item, TileArray> ores = new ObjectMap<>();
    /** Maps each team ID to a quarant. A quadrant is a grid of bits, where each bit is set if and only if there is a block of that team in that quadrant. */
    private GridBits[] structQuadrants;
    /** Stores all damaged tile entities by team. */
    private ObjectSet<Building>[] damagedTiles = new ObjectSet[Team.all.length];
    /** All ores available on this map. */
    private ObjectSet<Item> allOres = new ObjectSet<>();
    /** Stores teams that are present here as tiles. */
    private Seq<Team> activeTeams = new Seq<>(Team.class);
    /** Maps teams to a map of flagged tiles by flag. */
    private TileArray[][] flagMap = new TileArray[Team.all.length][BlockFlag.all.length];
    /** Max units by team. */
    private int[] unitCaps = new int[Team.all.length];
    /** Maps tile positions to their last known tile index data. */
    private IntMap<TileIndex> typeMap = new IntMap<>();
    /** Empty set used for returning. */
    private TileArray emptySet = new TileArray();
    /** Array used for returning and reusing. */
    private Seq<Tile> returnArray = new Seq<>();
    /** Array used for returning and reusing. */
    private Seq<Building> breturnArray = new Seq<>();

    public BlockIndexer(){
        Events.on(TileChangeEvent.class, event -> {
            updateIndices(event.tile);
        });

        Events.on(WorldLoadEvent.class, event -> {
            scanOres.clear();
            scanOres.addAll(Item.getAllOres());
            damagedTiles = new ObjectSet[Team.all.length];
            flagMap = new TileArray[Team.all.length][BlockFlag.all.length];
            unitCaps = new int[Team.all.length];
            activeTeams = new Seq<>(Team.class);

            for(int i = 0; i < flagMap.length; i++){
                for(int j = 0; j < BlockFlag.all.length; j++){
                    flagMap[i][j] = new TileArray();
                }
            }

            typeMap.clear();
            allOres.clear();
            ores = null;

            //create bitset for each team type that contains each quadrant
            structQuadrants = new GridBits[Team.all.length];

            for(Tile tile : world.tiles){
                process(tile);

                if(tile.build != null && tile.build.damaged()){
                    notifyTileDamaged(tile.build);
                }

                if(tile.drop() != null) allOres.add(tile.drop());
            }

            for(int x = 0; x < quadWidth(); x++){
                for(int y = 0; y < quadHeight(); y++){
                    updateQuadrant(world.tile(x * quadrantSize, y * quadrantSize));
                }
            }

            scanOres();
        });
    }

    public void updateIndices(Tile tile){
        if(typeMap.get(tile.pos()) != null){
            TileIndex index = typeMap.get(tile.pos());
            for(BlockFlag flag : index.flags){
                getFlagged(index.team)[flag.ordinal()].remove(tile);
            }

            if(index.flags.contains(BlockFlag.unitModifier)){
                updateCap(index.team);
            }
        }
        process(tile);
        updateQuadrant(tile);
    }

    private TileArray[] getFlagged(Team team){
        return flagMap[team.id];
    }

    private GridBits structQuadrant(Team t){
        if(structQuadrants[t.id] == null){
            structQuadrants[t.id] = new GridBits(Mathf.ceil(world.width() / (float)quadrantSize), Mathf.ceil(world.height() / (float)quadrantSize));
        }
        return structQuadrants[t.id];
    }

    /** Updates all the structure quadrants for a newly activated team. */
    public void updateTeamIndex(Team team){
        if(structQuadrants == null) return;

        //go through every tile... ouch
        for(Tile tile : world.tiles){
            if(tile.team() == team){
                int quadrantX = tile.x / quadrantSize;
                int quadrantY = tile.y / quadrantSize;
                structQuadrant(team).set(quadrantX, quadrantY);
            }
        }
    }

    /** @return whether this item is present on this map. */
    public boolean hasOre(Item item){
        return allOres.contains(item);
    }

    /** Returns all damaged tiles by team. */
    public ObjectSet<Building> getDamaged(Team team){
        breturnArray.clear();

        if(damagedTiles[team.id] == null){
            damagedTiles[team.id] = new ObjectSet<>();
        }

        ObjectSet<Building> set = damagedTiles[team.id];
        for(Building build : set){
            if((!build.isValid() || build.team != team || !build.damaged()) || build.block instanceof ConstructBlock){
                breturnArray.add(build);
            }
        }

        for(Building tile : breturnArray){
            set.remove(tile);
        }

        return set;
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

    public boolean eachBlock(Team team, float wx, float wy, float range, Boolf<Building> pred, Cons<Building> cons){
        intSet.clear();

        int tx = World.toTile(wx);
        int ty = World.toTile(wy);

        int tileRange = (int)(range / tilesize + 1);
        boolean any = false;

        for(int x = -tileRange + tx; x <= tileRange + tx; x++){
            for(int y = -tileRange + ty; y <= tileRange + ty; y++){
                if(!Mathf.within(x * tilesize, y * tilesize, wx, wy, range)) continue;

                Building other = world.build(x, y);

                if(other == null) continue;

                if((team == null || other.team == team) && pred.get(other) && intSet.add(other.pos())){
                    cons.get(other);
                    any = true;
                }
            }
        }

        return any;
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

    public void notifyTileDamaged(Building entity){
        if(damagedTiles[entity.team.id] == null){
            damagedTiles[entity.team.id] = new ObjectSet<>();
        }

        damagedTiles[entity.team.id].add(entity);
    }

    public Building findEnemyTile(Team team, float x, float y, float range, Boolf<Building> pred){
        for(int i = 0; i < activeTeams.size; i++){
            Team enemy = activeTeams.items[i];

            if(enemy == team || team == Team.derelict) continue;

            Building entity = indexer.findTile(enemy, x, y, range, pred, true);
            if(entity != null){
                return entity;
            }
        }

        return null;
    }

    public Building findTile(Team team, float x, float y, float range, Boolf<Building> pred){
        return findTile(team, x, y, range, pred, false);
    }

    public Building findTile(Team team, float x, float y, float range, Boolf<Building> pred, boolean usePriority){
        Building closest = null;
        float dst = 0;
        float range2 = range * range;

        for(int rx = Math.max((int)((x - range) / tilesize / quadrantSize), 0); rx <= (int)((x + range) / tilesize / quadrantSize) && rx < quadWidth(); rx++){
            for(int ry = Math.max((int)((y - range) / tilesize / quadrantSize), 0); ry <= (int)((y + range) / tilesize / quadrantSize) && ry < quadHeight(); ry++){

                if(!getQuad(team, rx, ry)) continue;

                for(int tx = rx * quadrantSize; tx < (rx + 1) * quadrantSize && tx < world.width(); tx++){
                    for(int ty = ry * quadrantSize; ty < (ry + 1) * quadrantSize && ty < world.height(); ty++){
                        Building e = world.build(tx, ty);

                        if(e == null || e.team != team || !pred.get(e) || !e.block.targetable || e.team == Team.derelict) continue;

                        float ndst = e.dst2(x, y);
                        if(ndst < range2 && (closest == null ||
                        //this one is closer, and it is at least of equal priority
                        (ndst < dst && (!usePriority || closest.block.priority.ordinal() <= e.block.priority.ordinal())) ||
                        //priority is used, and new block has higher priority regardless of range
                        (usePriority && closest.block.priority.ordinal() < e.block.priority.ordinal()))){
                            dst = ndst;
                            closest = e;
                        }
                    }
                }
            }
        }

        return closest;
    }

    /**
     * Returns a set of tiles that have ores of the specified type nearby.
     * While each tile in the set is not guaranteed to have an ore directly on it,
     * each tile will at least have an ore within {@link #quadrantSize} / 2 blocks of it.
     * Only specific ore types are scanned. See {@link #scanOres}.
     */
    public TileArray getOrePositions(Item item){
        return ores.get(item, emptySet);
    }

    /** Find the closest ore block relative to a position. */
    public Tile findClosestOre(float xp, float yp, Item item){
        Tile tile = Geometry.findClosest(xp, yp, getOrePositions(item));

        if(tile == null) return null;

        for(int x = Math.max(0, tile.x - quadrantSize / 2); x < tile.x + quadrantSize / 2 && x < world.width(); x++){
            for(int y = Math.max(0, tile.y - quadrantSize / 2); y < tile.y + quadrantSize / 2 && y < world.height(); y++){
                Tile res = world.tile(x, y);
                if(res.block() == Blocks.air && res.drop() == item){
                    return res;
                }
            }
        }

        return null;
    }

    /** Find the closest ore block relative to a position. */
    public Tile findClosestOre(Unit unit, Item item){
        return findClosestOre(unit.x, unit.y, item);
    }

    /** @return extra unit cap of a team. This is added onto the base value. */
    public int getExtraUnits(Team team){
        return unitCaps[team.id];
    }

    private void updateCap(Team team){
        TileArray capped = getFlagged(team)[BlockFlag.unitModifier.ordinal()];
        unitCaps[team.id] = 0;
        for(Tile capper : capped){
            unitCaps[team.id] += capper.block().unitCapModifier;
        }
    }

    private void process(Tile tile){
        if(tile.block().flags.size() > 0 && tile.team() != Team.derelict && tile.isCenter()){
            TileArray[] map = getFlagged(tile.team());

            for(BlockFlag flag : tile.block().flags){

                TileArray arr = map[flag.ordinal()];

                arr.add(tile);

                map[flag.ordinal()] = arr;
            }

            if(tile.block().flags.contains(BlockFlag.unitModifier)){
                updateCap(tile.team());
            }

            typeMap.put(tile.pos(), new TileIndex(tile.block().flags, tile.team()));
        }

        if(!activeTeams.contains(tile.team())){
            activeTeams.add(tile.team());
        }

        if(ores == null) return;

        int quadrantX = tile.x / quadrantSize;
        int quadrantY = tile.y / quadrantSize;
        itemSet.clear();

        Tile rounded = world.rawTile(Mathf.clamp(quadrantX * quadrantSize + quadrantSize / 2, 0, world.width() - 1), Mathf.clamp(quadrantY * quadrantSize + quadrantSize / 2, 0, world.height() - 1));

        //find all items that this quadrant contains
        for(int x = Math.max(0, rounded.x - quadrantSize / 2); x < rounded.x + quadrantSize / 2 && x < world.width(); x++){
            for(int y = Math.max(0, rounded.y - quadrantSize / 2); y < rounded.y + quadrantSize / 2 && y < world.height(); y++){
                Tile result = world.tile(x, y);
                if(result == null || result.drop() == null || !scanOres.contains(result.drop()) || result.block() != Blocks.air) continue;

                itemSet.add(result.drop());
            }
        }

        //update quadrant at this position
        for(Item item : scanOres){
            TileArray set = ores.get(item);

            //update quadrant status depending on whether the item is in it
            if(!itemSet.contains(item)){
                set.remove(rounded);
            }else{
                set.add(rounded);
            }
        }
    }

    private void updateQuadrant(Tile tile){
        if(structQuadrants == null) return;

        //this quadrant is now 'dirty', re-scan the whole thing
        int quadrantX = tile.x / quadrantSize;
        int quadrantY = tile.y / quadrantSize;

        for(Team team : activeTeams){
            GridBits bits = structQuadrant(team);

            //fast-set this quadrant to 'occupied' if the tile just placed is already of this team
            if(tile.team() == team && tile.build != null && tile.block().targetable){
                bits.set(quadrantX, quadrantY);
                continue; //no need to process futher
            }

            bits.set(quadrantX, quadrantY, false);

            outer:
            for(int x = quadrantX * quadrantSize; x < world.width() && x < (quadrantX + 1) * quadrantSize; x++){
                for(int y = quadrantY * quadrantSize; y < world.height() && y < (quadrantY + 1) * quadrantSize; y++){
                    Building result = world.build(x, y);
                    //when a targetable block is found, mark this quadrant as occupied and stop searching
                    if(result != null && result.team == team){
                        bits.set(quadrantX, quadrantY);
                        break outer;
                    }
                }
            }
        }
    }

    private boolean getQuad(Team team, int quadrantX, int quadrantY){
        return structQuadrant(team).get(quadrantX, quadrantY);
    }

    private int quadWidth(){
        return Mathf.ceil(world.width() / (float)quadrantSize);
    }

    private int quadHeight(){
        return Mathf.ceil(world.height() / (float)quadrantSize);
    }

    private void scanOres(){
        ores = new ObjectMap<>();

        //initialize ore map with empty sets
        for(Item item : scanOres){
            ores.put(item, new TileArray());
        }

        for(Tile tile : world.tiles){
            int qx = (tile.x / quadrantSize);
            int qy = (tile.y / quadrantSize);

            //add position of quadrant to list when an ore is found
            if(tile.drop() != null && scanOres.contains(tile.drop()) && tile.block() == Blocks.air){
                ores.get(tile.drop()).add(world.tile(
                //make sure to clamp quadrant middle position, since it might go off bounds
                Mathf.clamp(qx * quadrantSize + quadrantSize / 2, 0, world.width() - 1),
                Mathf.clamp(qy * quadrantSize + quadrantSize / 2, 0, world.height() - 1)));
            }
        }
    }

    private static class TileIndex{
        public final EnumSet<BlockFlag> flags;
        public final Team team;

        public TileIndex(EnumSet<BlockFlag> flags, Team team){
            this.flags = flags;
            this.team = team;
        }
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
