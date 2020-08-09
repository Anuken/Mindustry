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

    /** tile data, see PathTileStruct */
    private int[][] tiles;
    /** unordered array of path data for iteration only. DO NOT iterate or access this in the main thread. */
    private Seq<Flowfield> threadList = new Seq<>(), mainList = new Seq<>();
    /** Maps team ID and target to to a flowfield.*/
    private ObjectMap<PathTarget, Flowfield>[] fieldMap = new ObjectMap[Team.all.length];
    /** Used field maps. */
    private ObjectSet<PathTarget>[] fieldMapUsed = new ObjectSet[Team.all.length];
    /** handles task scheduling on the update thread. */
    private TaskQueue queue = new TaskQueue();
    /** Stores path target for a position. Main thread only.*/
    private ObjectMap<Position, PathTarget> targetCache = new ObjectMap<>();
    /** Current pathfinding thread */
    private @Nullable Thread thread;
    private IntSeq tmpArray = new IntSeq();

    public Pathfinder(){
        Events.on(WorldLoadEvent.class, event -> {
            stop();

            //reset and update internal tile array
            tiles = new int[world.width()][world.height()];
            fieldMap = new ObjectMap[Team.all.length];
            fieldMapUsed = new ObjectSet[Team.all.length];
            targetCache = new ObjectMap<>();
            threadList = new Seq<>();
            mainList = new Seq<>();

            for(Tile tile : world.tiles){
                tiles[tile.x][tile.y] = packTile(tile);
            }

            //special preset which may help speed things up; this is optional
            preloadPath(state.rules.waveTeam, FlagTarget.enemyCores);

            start();
        });

        Events.on(ResetEvent.class, event -> stop());

        Events.on(BuildinghangeEvent.class, event -> updateTile(event.tile));
    }

    /** Packs a tile into its internal representation. */
    private int packTile(Tile tile){
        return PathTile.get(tile.cost, (byte)tile.getTeamID(), !tile.solid() && tile.floor().drownTime <= 0f, !tile.solid() && tile.floor().isLiquid);
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
                    path.target.getPositions(path.team, path.targets);
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
                        if(data.target.refreshRate() > 0 && Time.timeSinceMillis(data.lastUpdateTime) > fieldTimeout){
                            //make sure it doesn't get removed twice
                            data.lastUpdateTime = Time.millis();

                            Team team = data.team;

                            Core.app.post(() -> {
                                //remove its used state
                                if(fieldMap[team.id] != null){
                                    fieldMap[team.id].remove(data.target);
                                    fieldMapUsed[team.id].remove(data.target);
                                }
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

    public @Nullable Tile getTargetTile(Tile tile, Team team, Position target){
        return getTargetTile(tile, team, getTarget(target));
    }

    /** Gets next tile to travel to. Main thread only. */
    public @Nullable Tile getTargetTile(Tile tile, Team team, PathTarget target){
        if(tile == null) return null;

        if(fieldMap[team.id] == null){
            fieldMap[team.id] = new ObjectMap<>();
            fieldMapUsed[team.id] = new ObjectSet<>();
        }

        Flowfield data = fieldMap[team.id].get(target);

        if(data == null){
            //if this combination is not found, create it on request
            if(fieldMapUsed[team.id].add(target)){
                //grab targets since this is run on main thread
                IntSeq targets = target.getPositions(team, new IntSeq());
                queue.post(() -> createPath(team, target, targets));
            }
            return tile;
        }

        //if refresh rate is positive, queue a refresh
        if(target.refreshRate() > 0 && Time.timeSinceMillis(data.lastUpdateTime) > target.refreshRate()){
            data.lastUpdateTime = Time.millis();

            tmpArray.clear();
            data.target.getPositions(data.team, tmpArray);

            synchronized(data.targets){
                //make sure the position actually changed
                if(!(data.targets.size == 1 && tmpArray.size == 1 && data.targets.first() == tmpArray.first())){
                    data.targets.clear();
                    data.target.getPositions(data.team, data.targets);

                    //queue an update
                    queue.post(() -> updateTargets(data));
                }
            }
        }

        int[][] values = data.weights;
        int value = values[tile.x][tile.y];

        Tile current = null;
        int tl = 0;
        for(Point2 point : Geometry.d8){
            int dx = tile.x + point.x, dy = tile.y + point.y;

            Tile other = world.tile(dx, dy);
            if(other == null) continue;

            if(values[dx][dy] < value && (current == null || values[dx][dy] < tl) && !other.solid() && other.floor().drownTime <= 0 &&
            !(point.x != 0 && point.y != 0 && (world.solid(tile.x + point.x, tile.y) || world.solid(tile.x, tile.y + point.y)))){ //diagonal corner trap
                current = other;
                tl = values[dx][dy];
            }
        }

        if(current == null || tl == impassable) return tile;

        return current;
    }

    private PathTarget getTarget(Position position){
        return targetCache.get(position, () -> new PositionTarget(position));
    }

    /** @return whether a tile can be passed through by this team. Pathfinding thread only. */
    private boolean passable(int x, int y, Team team){
        int tile = tiles[x][y];
        return PathTile.passable(tile) || (PathTile.team(tile) != team.id && PathTile.team(tile) != (int)Team.derelict.id);
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

        //assign impassability to the tile
        if(!passable(x, y, path.team)){
            path.weights[x][y] = impassable;
        }

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

    private void preloadPath(Team team, PathTarget target){
        updateFrontier(createPath(team, target, target.getPositions(team, new IntSeq())), -1);
    }

    /**
     * Created a new flowfield that aims to get to a certain target for a certain team.
     * Pathfinding thread only.
     */
    private Flowfield createPath(Team team, PathTarget target, IntSeq targets){
        Flowfield path = new Flowfield(team, target, world.width(), world.height());
        path.lastUpdateTime = Time.millis();

        threadList.add(path);

        //add to main thread's list of paths
        Core.app.post(() -> {
            mainList.add(path);
            if(fieldMap[team.id] != null){
                fieldMap[team.id].put(target, path);
            }
        });

        //grab targets from passed array
        synchronized(path.targets){
            path.targets.clear();
            path.targets.addAll(targets);
        }

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

        return path;
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
                    Tile other = world.tile(dx, dy);

                    if(other != null && (path.weights[dx][dy] > cost + other.cost || path.searches[dx][dy] < path.search) && passable(dx, dy, path.team)){
                        if(other.cost < 0) throw new IllegalArgumentException("Tile cost cannot be negative! " + other);
                        path.frontier.addFirst(Point2.pack(dx, dy));
                        path.weights[dx][dy] = cost + other.cost;
                        path.searches[dx][dy] = (short)path.search;
                    }
                }
            }
        }
    }

    /** A path target defines a set of targets for a path. */
    public enum FlagTarget implements PathTarget{
        enemyCores((team, out) -> {
            for(Tile other : indexer.getEnemy(team, BlockFlag.core)){
                out.add(other.pos());
            }

            //spawn points are also enemies.
            if(state.rules.waves && team == state.rules.defaultTeam){
                for(Tile other : spawner.getSpawns()){
                    out.add(other.pos());
                }
            }
        }),
        rallyPoints((team, out) -> {
            for(Tile other : indexer.getAllied(team, BlockFlag.rally)){
                out.add(other.pos());
            }
        });

        public static final FlagTarget[] all = values();

        private final Cons2<Team, IntSeq> targeter;

        FlagTarget(Cons2<Team, IntSeq> targeter){
            this.targeter = targeter;
        }

        @Override
        public IntSeq getPositions(Team team, IntSeq out){
            targeter.get(team, out);
            return out;
        }

        @Override
        public int refreshRate(){
            return 0;
        }
    }

    public static class PositionTarget implements PathTarget{
        public final Position position;

        public PositionTarget(Position position){
            this.position = position;
        }

        @Override
        public IntSeq getPositions(Team team, IntSeq out){
            out.add(Point2.pack(world.toTile(position.getX()), world.toTile(position.getY())));
            return out;
        }

        @Override
        public int refreshRate(){
            return 900;
        }
    }

    public interface PathTarget{
        /** Gets targets to pathfind towards. This must run on the main thread. */
        IntSeq getPositions(Team team, IntSeq out);
        /** Refresh rate in milliseconds. Return any number <= 0 to disable. */
        int refreshRate();
    }

    /** Data for a specific flow field to some set of destinations. */
    static class Flowfield{
        /** Team this path is for. */
        final Team team;
        /** Flag that is being targeted. */
        final PathTarget target;
        /** costs of getting to a specific tile */
        final int[][] weights;
        /** search IDs of each position - the highest, most recent search is prioritized and overwritten */
        final int[][] searches;
        /** search frontier, these are Pos objects */
        final IntQueue frontier = new IntQueue();
        /** all target positions; these positions have a cost of 0, and must be synchronized on! */
        final IntSeq targets = new IntSeq();
        /** current search ID */
        int search = 1;
        /** last updated time */
        long lastUpdateTime;

        Flowfield(Team team, PathTarget target, int width, int height){
            this.team = team;
            this.target = target;

            this.weights = new int[width][height];
            this.searches = new int[width][height];
            this.frontier.ensureCapacity((width + height) * 3);
        }
    }

    /** Holds a copy of tile data for a specific tile position. */
    @Struct
    class PathTileStruct{
        //traversal cost
        short cost;
        //team of block, if applicable (0 by default)
        byte team;
        //whether it's viable to pass this block
        boolean passable;
        //whether it's viable to pass this block through water
        boolean passableWater;
    }
}
