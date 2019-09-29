package io.anuke.mindustry.ai;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.function.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.arc.util.async.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.meta.*;

import static io.anuke.mindustry.Vars.*;

public class Pathfinder implements Runnable{
    private static final long maxUpdate = Time.millisToNanos(4);
    private static final int updateFPS = 60;
    private static final int updateInterval = 1000 / updateFPS;
    private static final int impassable = -1;

    /** tile data, see PathTileStruct */
    private int[][] tiles;
    /** unordered array of path data for iteration only. DO NOT iterate ot access this in the main thread.*/
    private Array<PathData> list = new Array<>();
    /** Maps teams + flags to a valid path to get to that flag for that team. */
    private PathData[][] pathMap = new PathData[Team.all.length][PathTarget.all.length];
    /** Grid map of created path data that should not be queued again. */
    private GridBits created = new GridBits(Team.all.length, PathTarget.all.length);
    /** handles task scheduling on the update thread. */
    private TaskQueue queue = new TaskQueue();
    /** current pathfinding thread */
    private @Nullable
    Thread thread;

    public Pathfinder(){
        Events.on(WorldLoadEvent.class, event -> {
            stop();

            //reset and update internal tile array
            tiles = new int[world.width()][world.height()];
            pathMap = new PathData[Team.all.length][PathTarget.all.length];
            created = new GridBits(Team.all.length, PathTarget.all.length);
            list = new Array<>();

            for(int x = 0; x < world.width(); x++){
                for(int y = 0; y < world.height(); y++){
                    tiles[x][y] = packTile(world.rawTile(x, y));
                }
            }

            //special preset which may help speed things up; this is optional
            preloadPath(waveTeam, PathTarget.enemyCores);

            start();
        });

        Events.on(ResetEvent.class, event -> stop());

        Events.on(TileChangeEvent.class, event -> updateTile(event.tile));
    }

    /** Packs a tile into its internal representation. */
    private int packTile(Tile tile){
        return PathTile.get(tile.cost, tile.getTeamID(), (byte)0, !tile.solid() && tile.floor().drownTime <= 0f);
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

    public int debugValue(Team team, int x, int y){
        if(pathMap[team.ordinal()][PathTarget.enemyCores.ordinal()] == null) return 0;
        return pathMap[team.ordinal()][PathTarget.enemyCores.ordinal()].weights[x][y];
    }

    /** Update a tile in the internal pathfinding grid. Causes a complete pathfinding reclaculation. */
    public void updateTile(Tile tile){
        if(net.client()) return;

        int x = tile.x, y = tile.y;

        tile.getLinkedTiles(t -> {
            if(Structs.inBounds(t.x, t.y, tiles)){
                tiles[t.x][t.y] = packTile(t);
            }
        });

        //can't iterate through array so use the map, which should not lead to problems
        for(PathData[] arr : pathMap){
            for(PathData path : arr){
                if(path != null){
                    synchronized(path.targets){
                        path.targets.clear();
                        path.target.getTargets(path.team, path.targets);
                    }
                }
            }
        }

        queue.post(() -> {
            for(PathData data : list){
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

                queue.run();

                //total update time no longer than maxUpdate
                for(PathData data : list){
                    updateFrontier(data, maxUpdate / list.size);
                }

                try{
                    Thread.sleep(updateInterval);
                }catch(InterruptedException e){
                    //stop looping when interrupted externally
                    return;
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    /** Gets next tile to travel to. Main thread only. */
    public Tile getTargetTile(Tile tile, Team team, PathTarget target){
        if(tile == null) return null;

        PathData data = pathMap[team.ordinal()][target.ordinal()];

        if(data == null){
            //if this combination is not found, create it on request
            if(!created.get(team.ordinal(), target.ordinal())){
                created.set(team.ordinal(), target.ordinal());
                //grab targets since this is run on main thread
                IntArray targets = target.getTargets(team, new IntArray());
                queue.post(() -> createPath(team, target, targets));
            }
            return tile;
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

    /** @return whether a tile can be passed through by this team. Pathfinding thread only.*/
    private boolean passable(int x, int y, Team team){
        int tile = tiles[x][y];
        return PathTile.passable(tile) || (PathTile.team(tile) != team.ordinal() && PathTile.team(tile) != Team.derelict.ordinal());
    }

    /**
     * Clears the frontier, increments the search and sets up all flow sources.
     * This only occurs for active teams.
     */
    private void updateTargets(PathData path, int x, int y){
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

        //increment search, clear frontier
        path.search++;
        path.frontier.clear();

        synchronized(path.targets){
            //add targets
            for(int i = 0; i < path.targets.size; i++){
                int pos = path.targets.get(i);
                int tx = Pos.x(pos), ty = Pos.y(pos);

                path.weights[tx][ty] = 0;
                path.searches[tx][ty] = (short)path.search;
                path.frontier.addFirst(pos);
            }
        }
    }

    private void preloadPath(Team team, PathTarget target){
        updateFrontier(createPath(team, target, target.getTargets(team, new IntArray())), -1);
    }

    /** Created a new flowfield that aims to get to a certain target for a certain team.
     * Pathfinding thread only. */
    private PathData createPath(Team team, PathTarget target, IntArray targets){
        PathData path = new PathData(team, target, world.width(), world.height());

        list.add(path);
        pathMap[team.ordinal()][target.ordinal()] = path;

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
            path.weights[Pos.x(pos)][Pos.y(pos)] = 0;
            path.frontier.addFirst(pos);
        }

        return path;
    }

    /** Update the frontier for a path. Pathfinding thread only. */
    private void updateFrontier(PathData path, long nsToRun){
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
                        path.frontier.addFirst(Pos.get(dx, dy));
                        path.weights[dx][dy] = cost + other.cost;
                        path.searches[dx][dy] = (short)path.search;
                    }
                }
            }
        }
    }

    /** A path target defines a set of targets for a path.*/
    public enum PathTarget{
        enemyCores((team, out) -> {
            for(Tile other : indexer.getEnemy(team, BlockFlag.core)){
                out.add(other.pos());
            }

            //spawn points are also enemies.
            if(state.rules.waves && team == defaultTeam){
                for(Tile other : spawner.getGroundSpawns()){
                    out.add(other.pos());
                }
            }
        }),
        rallyPoints((team, out) -> {
            for(Tile other : indexer.getAllied(team, BlockFlag.rally)){
                out.add(other.pos());
            }
        });

        public static final PathTarget[] all = values();

        private final BiConsumer<Team, IntArray> targeter;

        PathTarget(BiConsumer<Team, IntArray> targeter){
            this.targeter = targeter;
        }

        /** Get targets. This must run on the main thread.*/
        public IntArray getTargets(Team team, IntArray out){
            targeter.accept(team, out);
            return out;
        }
    }

    /** Data for a specific flow field to some set of destinations. */
    class PathData{
        /** Team this path is for. */
        final Team team;
        /** Flag that is being targeted. */
        final PathTarget target;
        /** costs of getting to a specific tile */
        final int[][] weights;
        /** search IDs of each position - the highest, most recent search is prioritized and overwritten */
        final short[][] searches;
        /** search frontier, these are Pos objects */
        final IntQueue frontier = new IntQueue();
        /** all target positions; these positions have a cost of 0, and must be synchronized on! */
        final IntArray targets = new IntArray();
        /** current search ID */
        int search = 1;

        PathData(Team team, PathTarget target, int width, int height){
            this.team = team;
            this.target = target;

            this.weights = new int[width][height];
            this.searches = new short[width][height];
            this.frontier.ensureCapacity((width + height) * 3);
        }
    }

    /** Holds a copy of tile data for a specific tile position. */
    @Struct
    class PathTileStruct{
        //traversal cost
        byte cost;
        //team of block, if applicable (0 by default)
        byte team;
        //type of target; TODO remove
        byte type;
        //whether it's viable to pass this block
        boolean passable;
    }
}
