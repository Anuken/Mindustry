package mindustry.ai;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

import static mindustry.Vars.*;
import static mindustry.ai.Pathfinder.*;

public class ControlPathfinder{
    //TODO this FPS-based update system could be flawed.
    private static final long maxUpdate = Time.millisToNanos(30);
    private static final int updateFPS = 60;
    private static final int updateInterval = 1000 / updateFPS;
    private static final int wallImpassableCap = 1_000_000;

    public static final PathCost

    costGround = (team, tile) ->
    //deep is impassable
    PathTile.allDeep(tile) ? impassable :
    //impassable same-team or neutral block
    PathTile.solid(tile) && ((PathTile.team(tile) == team && !PathTile.teamPassable(tile)) || PathTile.team(tile) == 0) ? impassable :
    //impassable synthetic enemy block
    ((PathTile.team(tile) != team && PathTile.team(tile) != 0) && PathTile.solid(tile) ? wallImpassableCap : 0) +
    1 +
    (PathTile.nearSolid(tile) ? 6 : 0) +
    (PathTile.nearLiquid(tile) ? 8 : 0) +
    (PathTile.deep(tile) ? 6000 : 0) +
    (PathTile.damages(tile) ? 50 : 0),

    //same as ground but ignores liquids/deep stuff
    costHover = (team, tile) ->
    //impassable same-team or neutral block
    PathTile.solid(tile) && ((PathTile.team(tile) == team && !PathTile.teamPassable(tile)) || PathTile.team(tile) == 0) ? impassable :
    //impassable synthetic enemy block
    ((PathTile.team(tile) != team && PathTile.team(tile) != 0) && PathTile.solid(tile) ? wallImpassableCap : 0) +
    1 +
    (PathTile.nearSolid(tile) ? 6 : 0),

    costLegs = (team, tile) ->
    PathTile.legSolid(tile) ? impassable : 1 +
    (PathTile.deep(tile) ? 6000 : 0) +
    (PathTile.nearSolid(tile) || PathTile.solid(tile) ? 3 : 0),

    costNaval = (team, tile) ->
    (PathTile.solid(tile) || !PathTile.liquid(tile) ? impassable : 1) +
    (PathTile.nearGround(tile) || PathTile.nearSolid(tile) ? 6 : 0);

    public static boolean showDebug = false;

    //static access probably faster than object access
    static int wwidth, wheight;
    //increments each tile change
    static volatile int worldUpdateId;

    /** Current pathfinding threads, contents may be null */
    @Nullable PathfindThread[] threads;
    /** for unique target IDs */
    int lastTargetId = 1;
    /** requests per-unit */
    ObjectMap<Unit, PathRequest> requests = new ObjectMap<>();

    public ControlPathfinder(){

        Events.on(WorldLoadEvent.class, event -> {
            stop();
            wwidth = world.width();
            wheight = world.height();

            start();
        });

        //only update the world when a solid block is removed or placed, everything else doesn't matter
        Events.on(TilePreChangeEvent.class, e -> {
            if(e.tile.solid()){
                worldUpdateId ++;
            }
        });

        Events.on(TileChangeEvent.class, e -> {
            if(e.tile.solid()){
                worldUpdateId ++;
            }
        });

        Events.on(ResetEvent.class, event -> stop());

        //invalidate paths
        Events.run(Trigger.update, () -> {
            for(var req : requests.values()){
                //skipped N update -> drop it
                if(req.lastUpdateId <= state.updateId - 10){
                    //concurrent modification!
                    Core.app.post(() -> requests.remove(req.unit));
                    req.thread.queue.post(() -> req.thread.requests.remove(req));
                }
            }
        });

        Events.run(Trigger.draw, () -> {
            if(!showDebug) return;

            for(var req : requests.values()){
                if(req.frontier == null) continue;
                Draw.draw(Layer.overlayUI, () -> {
                    if(req.done){
                        int len = req.result.size;
                        int rp = req.rayPathIndex;
                        if(rp < len && rp >= 0){
                            Draw.color(Color.royal);
                            Tile tile = tile(req.result.items[rp]);
                            Lines.line(req.unit.x, req.unit.y, tile.worldx(), tile.worldy());
                        }

                        for(int i = 0; i < len; i++){
                            Draw.color(Tmp.c1.set(Color.white).fromHsv(i / (float)len * 360f, 1f, 0.9f));
                            int pos = req.result.items[i];
                            Fill.square(pos % wwidth * tilesize, pos / wwidth * tilesize, 3f);

                            if(i == req.pathIndex){
                                Draw.color(Color.green);
                                Lines.square(pos % wwidth * tilesize, pos / wwidth * tilesize, 5f);
                            }
                        }
                    }else{
                        var view = Core.camera.bounds(Tmp.r1);
                        int len = req.frontier.size;
                        float[] weights = req.frontier.weights;
                        int[] poses = req.frontier.queue;
                        for(int i = 0; i < Math.min(len, 1000); i++){
                            int pos = poses[i];
                            if(view.contains(pos % wwidth * tilesize, pos / wwidth * tilesize)){
                                Draw.color(Tmp.c1.set(Color.white).fromHsv((weights[i] * 4f) % 360f, 1f, 0.9f));

                                Lines.square(pos % wwidth * tilesize, pos / wwidth * tilesize, 4f);
                            }
                        }
                    }
                    Draw.reset();
                });
            }
        });
    }


    /** @return the next target ID to use as a unique path identifier. */
    public int nextTargetId(){
        return lastTargetId ++;
    }

    /**
     * @return whether a path is ready.
     * @param pathId a unique ID for this location query, which should change every time the 'destination' vector is modified.
     * */
    public boolean getPathPosition(Unit unit, int pathId, Vec2 destination, Vec2 out){
        return getPathPosition(unit, pathId, destination, out, null);
    }

    /**
     * @return whether a path is ready.
     * @param pathId a unique ID for this location query, which should change every time the 'destination' vector is modified.
     * @param noResultFound extra return value for storing whether no valid path to the destination exists (thanks java!)
     * */
    public boolean getPathPosition(Unit unit, int pathId, Vec2 destination, Vec2 out, @Nullable boolean[] noResultFound){
        if(noResultFound != null){
            noResultFound[0] = false;
        }

        //uninitialized
        if(threads == null || !world.tiles.in(World.toTile(destination.x), World.toTile(destination.y))) return false;

        PathCost costType = unit.type.pathCost;
        int team = unit.team.id;

        //if the destination can be trivially reached in a straight line, do that.
        if((!requests.containsKey(unit) || requests.get(unit).curId != pathId) && !raycast(team, costType, unit.tileX(), unit.tileY(), World.toTile(destination.x), World.toTile(destination.y))){
            out.set(destination);
            return true;
        }

        //destination is impassable, can't go there.
        if(solid(team, costType, world.packArray(World.toTile(destination.x), World.toTile(destination.y)))){
            return false;
        }

        //check for request existence
        if(!requests.containsKey(unit)){
            PathfindThread thread = Structs.findMin(threads, t -> t.requestSize);

            var req = new PathRequest(thread);
            req.unit = unit;
            req.cost = costType;
            req.destination.set(destination);
            req.curId = pathId;
            req.team = team;
            req.lastUpdateId = state.updateId;
            req.lastPos.set(unit);
            req.lastWorldUpdate = worldUpdateId;
            //raycast immediately when done
            req.raycastTimer = 9999f;

            requests.put(unit, req);

            //add to thread so it gets processed next update
            thread.queue.post(() -> thread.requests.add(req));
        }else{
            var req = requests.get(unit);
            req.lastUpdateId = state.updateId;
            req.team = unit.team.id;
            if(req.curId != req.lastId || req.curId != pathId){
                req.pathIndex = 0;
                req.rayPathIndex = -1;
                req.done = false;
                req.foundEnd = false;
            }

            req.destination.set(destination);
            req.curId = pathId;

            //check for the unit getting stuck every N seconds
            if((req.stuckTimer += Time.delta) >= 60f * 2.5f){
                req.stuckTimer = 0f;
                //force recalculate
                if(req.lastPos.within(unit, 1.5f)){
                    req.lastWorldUpdate = -1;
                }
                req.lastPos.set(unit);
            }

            if(req.done){
                int[] items = req.result.items;
                int len = req.result.size;
                int tileX = unit.tileX(), tileY = unit.tileY();
                float range = 4f;

                float minDst = req.pathIndex < len ? unit.dst2(world.tiles.geti(items[req.pathIndex])) : 0f;
                int idx = req.pathIndex;

                //find closest node that is in front of the path index and hittable with raycast
                for(int i = len - 1; i >= idx; i--){
                    Tile tile = tile(items[i]);
                    float dst = unit.dst2(tile);
                    //TODO maybe put this on a timer since raycasts can be expensive?
                    if(dst < minDst && !permissiveRaycast(team, costType, tileX, tileY, tile.x, tile.y)){
                        req.pathIndex = Math.max(dst <= range * range ? i + 1 : i, req.pathIndex);
                        minDst = Math.min(dst, minDst);
                    }
                }

                if(req.rayPathIndex < 0){
                    req.rayPathIndex = req.pathIndex;
                }

                if((req.raycastTimer += Time.delta) >= 50f){
                    for(int i = len - 1; i > req.pathIndex; i--){
                        int val = items[i];
                        if(!raycast(team, costType, tileX, tileY, val % wwidth, val / wwidth)){
                            req.rayPathIndex = i;
                            break;
                        }
                    }
                    req.raycastTimer = 0;
                }

                if(req.rayPathIndex < len && req.rayPathIndex >= 0){
                    Tile tile = tile(items[req.rayPathIndex]);
                    out.set(tile);

                    if(unit.within(tile, range)){
                        req.pathIndex = req.rayPathIndex = Math.max(req.pathIndex, req.rayPathIndex + 1);
                    }
                }else{
                    //implicit done
                    out.set(unit);
                    //end of path, we're done here? reset path? what???
                }

                if(noResultFound != null){
                    noResultFound[0] = !req.foundEnd;
                }
            }

            return req.done;
        }

        return false;
    }
    /** Starts or restarts the pathfinding thread. */
    private void start(){
        stop();

        if(net.client()) return;

        //TODO currently capped at 6 threads, might be a good idea to make it more?
        threads = new PathfindThread[Mathf.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, 6)];
        for(int i = 0; i < threads.length; i ++){
            threads[i] = new PathfindThread("ControlPathfindThread-" + i);
            threads[i].setPriority(Thread.MIN_PRIORITY);
            threads[i].setDaemon(true);
            threads[i].start();
        }
    }

    /** Stops the pathfinding thread. */
    private void stop(){
        if(threads != null){
            for(var thread : threads){
                thread.interrupt();
            }
        }
        threads = null;
        requests.clear();
    }

    private static boolean raycast(int team, PathCost type, int x1, int y1, int x2, int y2){
        int ww = world.width(), wh = world.height();
        int x = x1, dx = Math.abs(x2 - x), sx = x < x2 ? 1 : -1;
        int y = y1, dy = Math.abs(y2 - y), sy = y < y2 ? 1 : -1;
        int e2, err = dx - dy;

        while(x >= 0 && y >= 0 && x < ww && y < wh){
            if(avoid(team, type, x + y * wwidth)) return true;
            if(x == x2 && y == y2) return false;

            //TODO no diagonals???? is this a good idea?
            /*
            //no diagonal ver
            if(2 * err + dy > dx - 2 * err){
                err -= dy;
                x += sx;
            }else{
                err += dx;
                y += sy;
            }*/

            //diagonal ver
            e2 = 2 * err;
            if(e2 > -dy){
                err -= dy;
                x += sx;
            }

            if(e2 < dx){
                err += dx;
                y += sy;
            }

        }

        return true;
    }

    private static boolean permissiveRaycast(int team, PathCost type, int x1, int y1, int x2, int y2){
        int ww = world.width(), wh = world.height();
        int x = x1, dx = Math.abs(x2 - x), sx = x < x2 ? 1 : -1;
        int y = y1, dy = Math.abs(y2 - y), sy = y < y2 ? 1 : -1;
        int err = dx - dy;

        while(x >= 0 && y >= 0 && x < ww && y < wh){
            if(solid(team, type, x + y * wwidth)) return true;
            if(x == x2 && y == y2) return false;

            //no diagonals
            if(2 * err + dy > dx - 2 * err){
                err -= dy;
                x += sx;
            }else{
                err += dx;
                y += sy;
            }
        }

        return true;
    }

    static boolean cast(int team, PathCost cost, int from, int to){
        return raycast(team, cost, from % wwidth, from / wwidth, to % wwidth, to / wwidth);
    }

    private Tile tile(int pos){
        return world.tiles.geti(pos);
    }

    //distance heuristic: manhattan
    private static float heuristic(int a, int b){
        int x = a % wwidth, x2 = b % wwidth, y = a / wwidth, y2 = b / wwidth;
        return Math.abs(x - x2) + Math.abs(y - y2);
    }

    private static int tcost(int team, PathCost cost, int tilePos){
        return cost.getCost(team, pathfinder.tiles[tilePos]);
    }

    private static int cost(int team, PathCost cost, int tilePos){
        if(state.rules.limitMapArea && !Team.get(team).isAI()){
            int x = tilePos % wwidth, y = tilePos / wwidth;
            if(x < state.rules.limitX || y < state.rules.limitY || x > state.rules.limitX + state.rules.limitWidth || y > state.rules.limitY + state.rules.limitHeight){
                return impassable;
            }
        }
        return cost.getCost(team, pathfinder.tiles[tilePos]);
    }

    private static boolean avoid(int team, PathCost type, int tilePos){
        int cost = cost(team, type, tilePos);
        return cost == impassable || cost >= 2;
    }

    private static boolean solid(int team, PathCost type, int tilePos){
        int cost = cost(team, type, tilePos);
        return cost == impassable || cost >= 6000;
    }

    private static float tileCost(int team, PathCost type, int a, int b){
        //currently flat cost
        return cost(team, type, b);
    }

    static class PathfindThread extends Thread{
        /** handles task scheduling on the update thread. */
        TaskQueue queue = new TaskQueue();
        /** pathfinding thread access only! */
        Seq<PathRequest> requests = new Seq<>();
        /** volatile for access across threads */
        volatile int requestSize;

        public PathfindThread(String name){
            super(name);
        }

        @Override
        public void run(){
            while(true){
                //stop on client, no updating
                if(net.client()) return;
                try{
                    if(state.isPlaying()){
                        queue.run();
                        requestSize = requests.size;

                        //total update time no longer than maxUpdate
                        for(var req : requests){
                            //TODO this is flawed with many paths
                            req.update(maxUpdate / requests.size);
                        }
                    }

                    try{
                        Thread.sleep(updateInterval);
                    }catch(InterruptedException e){
                        //stop looping when interrupted externally
                        return;
                    }
                }catch(Throwable e){
                    //do not crash the pathfinding thread
                    Log.err(e);
                }
            }
        }
    }

    static class PathRequest{
        final PathfindThread thread;

        volatile boolean done = false;
        volatile boolean foundEnd = false;
        volatile Unit unit;
        volatile PathCost cost;
        volatile int team;
        volatile int lastWorldUpdate;

        final Vec2 lastPos = new Vec2();
        float stuckTimer = 0f;

        final Vec2 destination = new Vec2();
        final Vec2 lastDestination = new Vec2();

        //TODO only access on main thread??
        volatile int pathIndex;

        int rayPathIndex = -1;
        IntSeq result = new IntSeq();
        volatile float raycastTimer;

        PathfindQueue frontier = new PathfindQueue();
        //node index -> node it came from
        IntIntMap cameFrom = new IntIntMap();
        //node index -> total cost
        IntFloatMap costs = new IntFloatMap();

        int start, goal;

        long lastUpdateId;
        long lastTime;

        volatile int lastId, curId;

        public PathRequest(PathfindThread thread){
            this.thread = thread;
        }

        void update(long maxUpdateNs){
            if(curId != lastId){
                clear(true);
            }
            lastId = curId;

            //re-do everything when world updates, but keep the old path around
            if(Time.timeSinceMillis(lastTime) > 1000 * 3 && (worldUpdateId != lastWorldUpdate || !destination.epsilonEquals(lastDestination, 2f))){
                lastTime = Time.millis();
                lastWorldUpdate = worldUpdateId;
                clear(false);
            }

            if(done) return;

            long ns = Time.nanos();
            int counter = 0;

            while(frontier.size > 0){
                int current = frontier.poll();

                if(current == goal){
                    foundEnd = true;
                    break;
                }

                int cx = current % wwidth, cy = current / wwidth;

                for(Point2 point : Geometry.d4){
                    int newx = cx + point.x, newy = cy + point.y;
                    int next = newx + wwidth * newy;

                    if(newx >= wwidth || newy >= wheight || newx < 0 || newy < 0) continue;

                    //in fallback mode, enemy walls are passable
                    if(tcost(team, cost, next) == impassable) continue;

                    float add = tileCost(team, cost, current, next);
                    float currentCost = costs.get(current);

                    if(add < 0) continue;

                    //the cost can include an impassable enemy wall, so cap the cost if so and add the base cost instead
                    //essentially this means that any path with enemy walls will only count the walls once, preventing strange behavior like avoiding based on wall count
                    float newCost = currentCost >= wallImpassableCap && add >= wallImpassableCap ? currentCost + add - wallImpassableCap : currentCost + add;

                    //a cost of 0 means "not set"
                    if(!costs.containsKey(next) || newCost < costs.get(next)){
                        costs.put(next, newCost);
                        float priority = newCost + heuristic(next, goal);
                        frontier.add(next, priority);
                        cameFrom.put(next, current);
                    }
                }

                //only check every N iterations to prevent nanoTime spam (slow)
                if((counter ++) >= 100){
                    counter = 0;

                    //exit when out of time.
                    if(Time.timeSinceNanos(ns) > maxUpdateNs){
                        return;
                    }
                }
            }

            lastTime = Time.millis();
            raycastTimer = 9999f;
            result.clear();

            pathIndex = 0;
            rayPathIndex = -1;

            if(foundEnd){
                int cur = goal;
                while(cur != start){
                    result.add(cur);
                    cur = cameFrom.get(cur);
                }

                result.reverse();

                smoothPath();
            }

            //don't keep this around in memory, better to dump entirely - using clear() keeps around massive arrays for paths
            frontier = new PathfindQueue();
            cameFrom = new IntIntMap();
            costs = new IntFloatMap();

            done = true;
        }

        void smoothPath(){
            int len = result.size;
            if(len <= 2) return;

            int output = 1, input = 2;

            while(input < len){
                if(cast(team, cost, result.get(output - 1), result.get(input))){
                    result.swap(output, input - 1);
                    output++;
                }
                input++;
            }

            result.swap(output, input - 1);
            result.size = output + 1;
        }

        void clear(boolean resetCurrent){
            done = false;

            frontier = new PathfindQueue(20);
            cameFrom.clear();
            costs.clear();

            start = world.packArray(unit.tileX(), unit.tileY());
            goal = world.packArray(World.toTile(destination.x), World.toTile(destination.y));

            cameFrom.put(start, start);
            costs.put(start, 0);

            frontier.add(start, 0);

            foundEnd = false;
            lastDestination.set(destination);

            if(resetCurrent){
                result.clear();
            }
        }
    }
}
