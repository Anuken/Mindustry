package mindustry.ai;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.type.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/** Class used for indexing special target blocks for AI. */
@SuppressWarnings("unchecked")
public class BlockIndexer{
    /** Size of one quadrant. */
    private final static int quadrantSize = 16;

    /** Set of all ores that are being scanned. */
    private final ObjectSet<Item> scanOres = new ObjectSet<>();
    private final ObjectSet<Item> itemSet = new ObjectSet<>();
    /** Stores all ore quadtrants on the map. */
    private ObjectMap<Item, ObjectSet<Tile>> ores = new ObjectMap<>();
    /** Maps each team ID to a quarant. A quadrant is a grid of bits, where each bit is set if and only if there is a block of that team in that quadrant. */
    private GridBits[] structQuadrants;
    /** Stores all damaged tile entities by team. */
    private ObjectSet<Tile>[] damagedTiles = new ObjectSet[Team.all().length];
    /**All ores available on this map.*/
    private ObjectSet<Item> allOres = new ObjectSet<>();
    /**Stores teams that are present here as tiles.*/
    private ObjectSet<Team> activeTeams = new ObjectSet<>();

    /** Maps teams to a map of flagged tiles by type. */
    private ObjectSet<Tile>[][] flagMap = new ObjectSet[Team.all().length][BlockFlag.all.length];
    /** Maps tile positions to their last known tile index data. */
    private IntMap<TileIndex> typeMap = new IntMap<>();
    /** Empty set used for returning. */
    private ObjectSet<Tile> emptySet = new ObjectSet<>();
    /** Array used for returning and reusing. */
    private Array<Tile> returnArray = new Array<>();

    public BlockIndexer(){
        Events.on(TileChangeEvent.class, event -> {
            if(typeMap.get(event.tile.pos()) != null){
                TileIndex index = typeMap.get(event.tile.pos());
                for(BlockFlag flag : index.flags){
                    getFlagged(index.team)[flag.ordinal()].remove(event.tile);
                }
            }
            process(event.tile);
            updateQuadrant(event.tile);
        });

        Events.on(WorldLoadEvent.class, event -> {
            scanOres.clear();
            scanOres.addAll(Item.getAllOres());
            damagedTiles = new ObjectSet[Team.all().length];
            flagMap = new ObjectSet[Team.all().length][BlockFlag.all.length];

            for(int i = 0; i < flagMap.length; i++){
                for(int j = 0; j < BlockFlag.all.length; j++){
                    flagMap[i][j] = new ObjectSet<>();
                }
            }

            typeMap.clear();
            allOres.clear();
            ores = null;

            //create bitset for each team type that contains each quadrant
            structQuadrants = new GridBits[Team.all().length];

            for(int x = 0; x < world.width(); x++){
                for(int y = 0; y < world.height(); y++){
                    Tile tile = world.tile(x, y);

                    process(tile);

                    if(tile.entity != null && tile.entity.damaged()){
                        notifyTileDamaged(tile.entity);
                    }

                    if(tile.drop() != null) allOres.add(tile.drop());
                }
            }

            for(int x = 0; x < quadWidth(); x++){
                for(int y = 0; y < quadHeight(); y++){
                    updateQuadrant(world.tile(x * quadrantSize, y * quadrantSize));
                }
            }

            scanOres();
        });
    }

    private ObjectSet<Tile>[] getFlagged(Team team){
        return flagMap[team.id];
    }

    private GridBits structQuadrant(Team t){
        int id = Pack.u(t.id);
        if(structQuadrants[id] == null){
            structQuadrants[id] = new GridBits(Mathf.ceil(world.width() / (float)quadrantSize), Mathf.ceil(world.height() / (float)quadrantSize));
        }
        return structQuadrants[id];
    }

    /** Updates all the structure quadrants for a newly activated team. */
    public void updateTeamIndex(Team team){
        if(structQuadrants == null) return;

        //go through every tile... ouch
        for(int x = 0; x < world.width(); x++){
            for(int y = 0; y < world.height(); y++){
                Tile tile = world.tile(x, y);
                if(tile.getTeam() == team){
                    int quadrantX = tile.x / quadrantSize;
                    int quadrantY = tile.y / quadrantSize;
                    structQuadrant(team).set(quadrantX, quadrantY);
                }
            }
        }
    }

    /** @return whether this item is present on this map.*/
    public boolean hasOre(Item item){
        return allOres.contains(item);
    }

    /** Returns all damaged tiles by team. */
    public ObjectSet<Tile> getDamaged(Team team){
        returnArray.clear();

        if(damagedTiles[team.id] == null){
            damagedTiles[team.id] = new ObjectSet<>();
        }

        ObjectSet<Tile> set = damagedTiles[team.id];
        for(Tile tile : set){
            if((tile.entity == null || tile.entity.getTeam() != team || !tile.entity.damaged()) || tile.block() instanceof BuildBlock){
                returnArray.add(tile);
            }
        }

        for(Tile tile : returnArray){
            set.remove(tile);
        }

        return set;
    }

    /** Get all allied blocks with a flag. */
    public ObjectSet<Tile> getAllied(Team team, BlockFlag type){
        return flagMap[team.id][type.ordinal()];
    }

    /** Get all enemy blocks with a flag. */
    public Array<Tile> getEnemy(Team team, BlockFlag type){
        returnArray.clear();
        for(Team enemy : team.enemies()){
            if(state.teams.isActive(enemy)){
                ObjectSet<Tile> set = getFlagged(enemy)[type.ordinal()];
                if(set != null){
                    for(Tile tile : set){
                        returnArray.add(tile);
                    }
                }
            }
        }
        return returnArray;
    }

    public void notifyTileDamaged(TileEntity entity){
        if(damagedTiles[(int)entity.getTeam().id] == null){
            damagedTiles[(int)entity.getTeam().id] = new ObjectSet<>();
        }

        ObjectSet<Tile> set = damagedTiles[(int)entity.getTeam().id];
        set.add(entity.tile);
    }

    public TileEntity findEnemyTile(Team team, float x, float y, float range, Boolf<Tile> pred){
        for(Team enemy : activeTeams){
            if(!team.isEnemy(enemy)) continue;

            TileEntity entity = indexer.findTile(enemy, x, y, range, pred, true);
            if(entity != null){
                return entity;
            }
        }

        return null;
    }

    public TileEntity findTile(Team team, float x, float y, float range, Boolf<Tile> pred){
        return findTile(team, x, y, range, pred, false);
    }

    public TileEntity findTile(Team team, float x, float y, float range, Boolf<Tile> pred, boolean usePriority){
        TileEntity closest = null;
        float dst = 0;
        float range2 = range*range;

        for(int rx = Math.max((int)((x - range) / tilesize / quadrantSize), 0); rx <= (int)((x + range) / tilesize / quadrantSize) && rx < quadWidth(); rx++){
            for(int ry = Math.max((int)((y - range) / tilesize / quadrantSize), 0); ry <= (int)((y + range) / tilesize / quadrantSize) && ry < quadHeight(); ry++){

                if(!getQuad(team, rx, ry)) continue;

                for(int tx = rx * quadrantSize; tx < (rx + 1) * quadrantSize && tx < world.width(); tx++){
                    for(int ty = ry * quadrantSize; ty < (ry + 1) * quadrantSize && ty < world.height(); ty++){
                        Tile other = world.ltile(tx, ty);

                        if(other == null) continue;

                        if(other.entity == null || other.getTeam() != team || !pred.get(other) || !other.block().targetable)
                            continue;

                        TileEntity e = other.entity;

                        float ndst = Mathf.dst2(x, y, e.x, e.y);
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
    public ObjectSet<Tile> getOrePositions(Item item){
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

    private void process(Tile tile){
        if(tile.block().flags.size() > 0 && tile.getTeam() != Team.derelict){
            ObjectSet<Tile>[] map = getFlagged(tile.getTeam());

            for(BlockFlag flag : tile.block().flags){

                ObjectSet<Tile> arr = map[flag.ordinal()];

                arr.add(tile);

                map[flag.ordinal()] = arr;
            }
            typeMap.put(tile.pos(), new TileIndex(tile.block().flags, tile.getTeam()));
        }
        activeTeams.add(tile.getTeam());

        if(ores == null) return;

        int quadrantX = tile.x / quadrantSize;
        int quadrantY = tile.y / quadrantSize;
        itemSet.clear();

        Tile rounded = world.tile(Mathf.clamp(quadrantX * quadrantSize + quadrantSize / 2, 0, world.width() - 1), Mathf.clamp(quadrantY * quadrantSize + quadrantSize / 2, 0, world.height() - 1));

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
        if(structQuadrants == null) return;

        //this quadrant is now 'dirty', re-scan the whole thing
        int quadrantX = tile.x / quadrantSize;
        int quadrantY = tile.y / quadrantSize;

        for(Team team : activeTeams){
            GridBits bits = structQuadrant(team);

            //fast-set this quadrant to 'occupied' if the tile just placed is already of this team
            if(tile.getTeam() == team && tile.entity != null && tile.block().targetable){
                bits.set(quadrantX, quadrantY);
                continue; //no need to process futher
            }

            bits.set(quadrantX, quadrantY, false);

            outer:
            for(int x = quadrantX * quadrantSize; x < world.width() && x < (quadrantX + 1) * quadrantSize; x++){
                for(int y = quadrantY * quadrantSize; y < world.height() && y < (quadrantY + 1) * quadrantSize; y++){
                    Tile result = world.ltile(x, y);
                    //when a targetable block is found, mark this quadrant as occupied and stop searching
                    if(result.entity != null && result.getTeam() == team){
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
            ores.put(item, new ObjectSet<>());
        }

        for(int x = 0; x < world.width(); x++){
            for(int y = 0; y < world.height(); y++){
                int qx = (x / quadrantSize);
                int qy = (y / quadrantSize);

                Tile tile = world.tile(x, y);

                //add position of quadrant to list when an ore is found
                if(tile.drop() != null && scanOres.contains(tile.drop()) && tile.block() == Blocks.air){
                    ores.get(tile.drop()).add(world.tile(
                    //make sure to clamp quadrant middle position, since it might go off bounds
                    Mathf.clamp(qx * quadrantSize + quadrantSize / 2, 0, world.width() - 1),
                    Mathf.clamp(qy * quadrantSize + quadrantSize / 2, 0, world.height() - 1)));
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
