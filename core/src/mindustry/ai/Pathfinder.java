package mindustry.ai;

import arc.*;
import arc.func.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import arc.util.async.*;
import mindustry.annotations.Annotations.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Pathfinder implements Runnable{
    private static final long maxUpdate = Time.millisToNanos(6);
    private static final int updateFPS = 60;
    private static final int updateInterval = 1000 / updateFPS;
    private static final int impassable = -1;
    private static final int fieldTimeout = 1000 * 60 * 2;

    public static final int
        fieldCore = 0,
        fieldRally = 1;

    public static final Seq<Prov<Flowfield>> fieldTypes = Seq.with(
        EnemyCoreField::new,
        RallyField::new
    );

    public static final int
        costGround = 0,
        costLegs = 1,
        costWater = 2;

    public static final Seq<PathCost> costTypes = Seq.with(
        //ground
        (team, tile) -> (PathTile.team(tile) == team.id || PathTile.team(tile) == 0) && PathTile.solid(tile) ? impassable : 1 +
            PathTile.health(tile) * 5 +
            (PathTile.nearSolid(tile) ? 2 : 0) +
            (PathTile.nearLiquid(tile) ? 6 : 0) +
            (PathTile.deep(tile) ? 70 : 0),

        //legs
        (team, tile) -> PathTile.legSolid(tile) ? impassable : 1 +
            (PathTile.solid(tile) ? 5 : 0),

        //water
        (team, tile) -> PathTile.solid(tile) || !PathTile.liquid(tile) ? 200 : 2 + //TODO cannot go through blocks - pathfinding isn't great
            (PathTile.nearGround(tile) || PathTile.nearSolid(tile) ? 14 : 0) +
            (PathTile.deep(tile) ? -1 : 0)
    );

    //maps team, cost, type to flow field
    Flowfield[][][] cache;

    /** tile data, see PathTileStruct */
    int[][] tiles = new int[0][0];
    /** unordered array of path data for iteration only. DO NOT iterate or access this in the main thread. */
    Seq<Flowfield> threadList = new Seq<>(), mainList = new Seq<>();
    /** handles task scheduling on the update thread. */
    TaskQueue queue = new TaskQueue();
    /** Current pathfinding thread */
    @Nullable Thread thread;
    IntSeq tmpArray = new IntSeq();

    public Pathfinder(){
        clearCache();

        Events.on(WorldLoadEvent.class, event -> {
            stop();

            //reset and update internal tile array
            tiles = new int[world.width()][world.height()];
            threadList = new Seq<>();
            mainList = new Seq<>();
            clearCache();

            for(Tile tile : world.tiles){
                tiles[tile.x][tile.y] = packTile(tile);
            }

            //special preset which may help speed things up; this is optional
            preloadPath(getField(state.rules.waveTeam, costGround, fieldCore));

            start();
        });

        Events.on(ResetEvent.class, event -> stop());

        Events.on(TileChangeEvent.class, event -> updateTile(event.tile));
    }

    private void clearCache(){
        cache = new Flowfield[256][5][5];
    }

    /** Packs a tile into its internal representation. */
    private int packTile(Tile tile){
        //TODO nearGround is just the inverse of nearLiquid?
        boolean nearLiquid = false, nearSolid = false, nearGround = false;

        for(int i = 0; i < 4; i++){
            Tile other = tile.getNearby(i);
            if(other != null){
                if(other.floor().isLiquid) nearLiquid = true;
                if(other.solid()) nearSolid = true;
                if(!other.floor().isLiquid) nearGround = true;
            }
        }

        return PathTile.get(
            tile.build == null ? 0 : Math.min((int)(tile.build.health / 40), 127),
            tile.getTeamID(),
            tile.solid(),
            tile.floor().isLiquid,
            tile.staticDarkness() >= 2,
            nearLiquid,
            nearGround,
            nearSolid,
            tile.floor().isDeep()
        );
    }

    /** Starts or restarts the pathfinding thread. */
    private void start(){
        stop();
        thread = Threads.daemon(this);
    }

    /** Stops the pathfinding thread. */
    private void stop(){
        if(thread != null){
            thread.interrupt();
            thread = null;
        }
        queue.clear();
    }

    /** Update a tile in the internal pathfinding grid.
     * Causes a complete pathfinding reclaculation. Main thread only. */
    public void updateTile(Tile tile){
        if(net.client()) return;

        int x = tile.x, y = tile.y;

        tile.getLinkedTiles(t -> {
            if(Structs.inBounds(t.x, t.y, tiles)){
                tiles[t.x][t.y] = packTile(t);
            }
        });

        //can't iterate through array so use the map, which should not lead to problems
        for(Flowfield path : mainList){
            if(path != null){
                synchronized(path.targets){
                    path.targets.clear();
                    path.getPositions(path.targets);
                }
            }
        }

        queue.post(() -> {
            for(Flowfield data : threadList){
                updateTargets(data, x, y);
            }
        });
    }

    /** Thread implementation. */
    @Override
    public void run(){
        while(true){
            if(net.client()) return;
            try{

                if(state.isPlaying()){
                    queue.run();

                    //total update time no longer than maxUpdate
                    for(Flowfield data : threadList){
                        updateFrontier(data, maxUpdate / threadList.size);

                        //remove flowfields that have 'timed out' so they can be garbage collected and no longer waste space
                        if(data.refreshRate > 0 && Time.timeSinceMillis(data.lastUpdateTime) > fieldTimeout){
                            //make sure it doesn't get removed twice
                            data.lastUpdateTime = Time.millis();

                            Team team = data.team;

                            Core.app.post(() -> {
                                //TODO ?????
                                //remove its used state
                                //if(fieldMap[team.id] != null){
                                //    fieldMap[team.id].remove(data.target);
                                //    fieldMapUsed[team.id].remove(data.target);
                                //}
                                //remove from main thread list
                                mainList.remove(data);
                            });

                            queue.post(() -> {
                                //remove from this thread list with a delay
                                threadList.remove(data);
                            });
                        }
                    }
                }

                try{
                    Thread.sleep(updateInterval);
                }catch(InterruptedException e){
                    //stop looping when interrupted externally
                    return;
                }
            }catch(Throwable e){
                e.printStackTrace();
            }
        }
    }

    public Flowfield getField(Team team, int costType, int fieldType){
        if(cache[team.id][costType][fieldType] == null){
            Flowfield field = fieldTypes.get(fieldType).get();
            field.team = team;
            field.cost = costTypes.get(costType);
            field.targets.clear();
            field.getPositions(field.targets);

            cache[team.id][costType][fieldType] = field;
            queue.post(() -> registerPath(field));
        }
        return cache[team.id][costType][fieldType];
    }

    /** Gets next tile to travel to. Main thread only. */
    public @Nullable Tile getTargetTile(Tile tile, Flowfield path){
        if(tile == null) return null;

        //uninitialized flowfields are not applicable
        if(!path.initialized){
            return tile;
        }

        //if refresh rate is positive, queue a refresh
        if(path.refreshRate > 0 && Time.timeSinceMillis(path.lastUpdateTime) > path.refreshRate){
            path.lastUpdateTime = Time.millis();

            tmpArray.clear();
            path.getPositions(tmpArray);

            synchronized(path.targets){
                //make sure the position actually changed
                if(!(path.targets.size == 1 && tmpArray.size == 1 && path.targets.first() == tmpArray.first())){
                    path.targets.clear();
                    path.getPositions(path.targets);

                    //queue an update
                    queue.post(() -> updateTargets(path));
                }
            }
        }

        int[][] values = path.weights;
        int value = values[tile.x][tile.y];

        Tile current = null;
        int tl = 0;
        for(Point2 point : Geometry.d8){
            int dx = tile.x + point.x, dy = tile.y + point.y;

            Tile other = world.tile(dx, dy);
            if(other == null) continue;

            if(values[dx][dy] < value && (current == null || values[dx][dy] < tl) && path.passable(dx, dy) &&
            !(point.x != 0 && point.y != 0 && (!path.passable(tile.x + point.x, tile.y) || !path.passable(tile.x, tile.y + point.y)))){ //diagonal corner trap
                current = other;
                tl = values[dx][dy];
            }
        }

        if(current == null || tl == impassable) return tile;

        return current;
    }

    /**
     * Clears the frontier, increments the search and sets up all flow sources.
     * This only occurs for active teams.
     */
    private void updateTargets(Flowfield path, int x, int y){
        if(!Structs.inBounds(x, y, path.weights)) return;

        if(path.weights[x][y] == 0){
            //this was a previous target
            path.frontier.clear();
        }else if(!path.frontier.isEmpty()){
            //skip if this path is processing
            return;
        }

        //update cost of the tile TODO maybe only update the cost when it's not passable
        path.weights[x][y] = path.cost.getCost(path.team, tiles[x][y]);

        //clear frontier to prevent contamination
        path.frontier.clear();

        updateTargets(path);
    }

    /** Increments the search and sets up flow sources. Does not change the frontier. */
    private void updateTargets(Flowfield path){

        //increment search, but do not clear the frontier
        path.search++;

        synchronized(path.targets){
            //add targets
            for(int i = 0; i < path.targets.size; i++){
                int pos = path.targets.get(i);
                int tx = Point2.x(pos), ty = Point2.y(pos);

                path.weights[tx][ty] = 0;
                path.searches[tx][ty] = path.search;
                path.frontier.addFirst(pos);
            }
        }
    }

    private void preloadPath(Flowfield path){
        path.targets.clear();
        path.getPositions(path.targets);
        registerPath(path);
        updateFrontier(path, -1);
    }

    /**
     * TODO wrong docs
     * Created a new flowfield that aims to get to a certain target for a certain team.
     * Pathfinding thread only.
     */
    private void registerPath(Flowfield path){
        path.lastUpdateTime = Time.millis();
        path.setup(tiles.length, tiles[0].length);

        threadList.add(path);

        //add to main thread's list of paths
        Core.app.post(() -> {
            mainList.add(path);
            //TODO
            //if(fieldMap[team.id] != null){
            //    fieldMap[team.id].put(target, path);
            //}
        });

        //fill with impassables by default
        for(int x = 0; x < world.width(); x++){
            for(int y = 0; y < world.height(); y++){
                path.weights[x][y] = impassable;
            }
        }

        //add targets
        for(int i = 0; i < path.targets.size; i++){
            int pos = path.targets.get(i);
            path.weights[Point2.x(pos)][Point2.y(pos)] = 0;
            path.frontier.addFirst(pos);
        }
    }

    /** Update the frontier for a path. Pathfinding thread only. */
    private void updateFrontier(Flowfield path, long nsToRun){
        long start = Time.nanos();

        while(path.frontier.size > 0 && (nsToRun < 0 || Time.timeSinceNanos(start) <= nsToRun)){
            Tile tile = world.tile(path.frontier.removeLast());
            if(tile == null || path.weights == null) return; //something went horribly wrong, bail
            int cost = path.weights[tile.x][tile.y];

            //pathfinding overflowed for some reason, time to bail. the next block update will handle this, hopefully
            if(path.frontier.size >= world.width() * world.height()){
                path.frontier.clear();
                return;
            }

            if(cost != impassable){
                for(Point2 point : Geometry.d4){

                    int dx = tile.x + point.x, dy = tile.y + point.y;

                    if(dx < 0 || dy < 0 || dx >= tiles.length || dy >= tiles[0].length) continue;

                    int otherCost = path.cost.getCost(path.team, tiles[dx][dy]);

                    if((path.weights[dx][dy] > cost + otherCost || path.searches[dx][dy] < path.search) && otherCost != impassable){
                        path.frontier.addFirst(Point2.pack(dx, dy));
                        path.weights[dx][dy] = cost + otherCost;
                        path.searches[dx][dy] = (short)path.search;
                    }
                }
            }
        }
    }

    public static class EnemyCoreField extends Flowfield{
        @Override
        protected void getPositions(IntSeq out){
            for(Tile other : indexer.getEnemy(team, BlockFlag.core)){
                out.add(other.pos());
            }

            //spawn points are also enemies.
            if(state.rules.waves && team == state.rules.defaultTeam){
                for(Tile other : spawner.getSpawns()){
                    out.add(other.pos());
                }
            }
        }
    }

    public static class RallyField extends Flowfield{
        @Override
        protected void getPositions(IntSeq out){
            for(Tile other : indexer.getAllied(team, BlockFlag.rally)){
                out.add(other.pos());
            }
        }
    }

    public static class PositionTarget extends Flowfield{
        public final Position position;

        public PositionTarget(Position position){
            this.position = position;
            this.refreshRate = 900;
        }

        @Override
        public void getPositions(IntSeq out){
            out.add(Point2.pack(world.toTile(position.getX()), world.toTile(position.getY())));
        }

    }

    /**
     * Data for a flow field to some set of destinations.
     * Concrete subclasses must specify a way to fetch costs and destinations.
     * */
    static abstract class Flowfield{
        /** Refresh rate in milliseconds. Return any number <= 0 to disable. */
        protected int refreshRate;
        /** Team this path is for. Set before using. */
        protected Team team = Team.derelict;
        /** Function for calculating path cost. Set before using. */
        protected PathCost cost = costTypes.get(costGround);

        /** costs of getting to a specific tile */
        int[][] weights;
        /** search IDs of each position - the highest, most recent search is prioritized and overwritten */
        int[][] searches;
        /** search frontier, these are Pos objects */
        IntQueue frontier = new IntQueue();
        /** all target positions; these positions have a cost of 0, and must be synchronized on! */
        IntSeq targets = new IntSeq();
        /** current search ID */
        int search = 1;
        /** last updated time */
        long lastUpdateTime;
        /** whether this flow field is ready to be used */
        boolean initialized;

        void setup(int width, int height){
            this.weights = new int[width][height];
            this.searches = new int[width][height];
            this.frontier.ensureCapacity((width + height) * 3);
            this.initialized = true;
        }

        protected boolean passable(int x, int y){
            return cost.getCost(team, pathfinder.tiles[x][y]) != impassable;
        }

        /** Gets targets to pathfind towards. This must run on the main thread. */
        protected abstract void getPositions(IntSeq out);
    }

    interface PathCost{
        int getCost(Team traversing, int tile);
    }

    /** Holds a copy of tile data for a specific tile position. */
    @Struct
    class PathTileStruct{
        //scaled block health
        @StructField(8) int health;
        //team of block, if applicable (0 by default)
        @StructField(8) int team;
        //general solid state
        boolean solid;
        //whether this block is a liquid that boats can move on
        boolean liquid;
        //whether this block is solid for leg units that can move over some solid blocks
        boolean legSolid;
        //whether this block is near liquids
        boolean nearLiquid;
        //whether this block is near a solid floor tile
        boolean nearGround;
        //whether this block is near a solid object
        boolean nearSolid;
        //whether this block is deep / drownable
        boolean deep;
    }
}
