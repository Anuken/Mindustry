package mindustry.ai;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.async.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

import static mindustry.Vars.*;
import static mindustry.ai.Pathfinder.*;

//TODO I'm sure this class has countless problems
public class ControlPathfinder implements Runnable{
    private static final long maxUpdate = Time.millisToNanos(20);
    private static final int updateFPS = 60;
    private static final int updateInterval = 1000 / updateFPS;

    public static boolean showDebug = false;

    public static final Seq<PathCost> costTypes = Seq.with(
        //ground
        (team, tile) -> (PathTile.allDeep(tile) || PathTile.solid(tile)) ? impassable : 1 +
        (PathTile.nearSolid(tile) ? 6 : 0) +
        (PathTile.nearLiquid(tile) ? 8 : 0) +
        (PathTile.deep(tile) ? 6000 : 0) +
        (PathTile.damages(tile) ? 40 : 0),

        //legs
        (team, tile) -> PathTile.legSolid(tile) ? impassable : 1 +
        (PathTile.deep(tile) ? 6000 : 0) +
        (PathTile.nearSolid(tile) || PathTile.solid(tile) ? 3 : 0),

        //water
        (team, tile) -> (PathTile.solid(tile) || !PathTile.liquid(tile) ? impassable : 1) +
        (PathTile.nearGround(tile) || PathTile.nearSolid(tile) ? 2 : 0) +
        (PathTile.deep(tile) ? 0 : 1)
    );

    //static access probably faster than object access
    static int wwidth, wheight;
    //increments each tile change
    static volatile int worldUpdateId;

    /** Current pathfinding thread */
    @Nullable Thread thread;
    /** for unique target IDs */
    int lastTargetId = 1;
    /** handles task scheduling on the update thread. */
    TaskQueue queue = new TaskQueue();
    /** requests per-unit */
    ObjectMap<Unit, PathRequest> requests = new ObjectMap<>();

    /** pathfinding thread access only! */
    Seq<PathRequest> threadRequests = new Seq<>();

    public ControlPathfinder(){
        Events.on(WorldLoadEvent.class, event -> {
            stop();
            wwidth = world.width();
            wheight = world.height();

            start();
        });

        Events.on(TileChangeEvent.class, e -> {
            worldUpdateId ++;
        });

        Events.on(ResetEvent.class, event -> stop());

        //invalidate paths
        Events.run(Trigger.update, () -> {
            for(var req : requests.values()){
                //skipped N update -> drop it
                if(req.lastUpdateId <= state.updateId - 10){
                    requests.remove(req.unit);
                    queue.post(() -> threadRequests.remove(req));
                }
            }
        });

        Events.run(Trigger.draw, () -> {
            if(!showDebug) return;

            for(var req : requests.values()){
                if(req.frontier == null) continue;
                //TODO will this even work
                Draw.draw(Layer.overlayUI, () -> {
                    if(req.done){
                        int len = req.result.size;
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
                        int len = req.frontier.size;
                        float[] weights = req.frontier.weights;
                        int[] poses = req.frontier.queue;
                        for(int i = 0; i < len; i++){
                            Draw.color(Tmp.c1.set(Color.white).fromHsv((weights[i] * 4f) % 360f, 1f, 0.9f));
                            int pos = poses[i];
                            Lines.square(pos % wwidth * tilesize, pos / wwidth * tilesize, 4f);
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

    /** @return whether a path is ready */
    public boolean getPathPosition(Unit unit, int pathId, Vec2 destination, Vec2 out){

        int pathType = unit.pathType();

        //if the destination can be trivially reached in a straight line, do that.
        if((!requests.containsKey(unit) || requests.get(unit).curId != pathId) && !raycast(pathType, unit.tileX(), unit.tileY(), World.toTile(destination.x), World.toTile(destination.y))){
            out.set(destination);
            return true;
        }

        //destination is impassable, can't go there.
        if(solid(pathType, world.packArray(World.toTile(destination.x), World.toTile(destination.y)))){
            return false;
        }

        //check for request existence
        if(!requests.containsKey(unit)){
            var req = new PathRequest();
            req.unit = unit;
            req.pathType = pathType;
            req.destination.set(destination);
            req.curId = pathId;
            req.lastUpdateId = state.updateId;
            req.lastPos.set(unit);
            req.lastWorldUpdate = worldUpdateId;

            requests.put(unit, req);

            //add to thread so it gets processed next update
            queue.post(() -> threadRequests.add(req));
        }else{
            var req = requests.get(unit);
            req.lastUpdateId = state.updateId;
            if(req.curId != req.lastId || req.curId != pathId){
                req.pathIndex = 0;
                req.rayPathIndex = -1;
                req.done = false;
                req.foundEnd = false;
            }

            req.destination.set(destination);
            req.curId = pathId;

            //check for the unit getting stuck every N seconds
            if((req.stuckTimer += Time.delta) >= 60f * 5f){
                req.stuckTimer = 0f;
                //force recalculate
                if(req.lastPos.within(unit, 1f)){
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
                    if(dst < minDst && !permissiveRaycast(pathType, tileX, tileY, tile.x, tile.y)){
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
                        if(!raycast(pathType, tileX, tileY, val % wwidth, val / wwidth)){
                            req.rayPathIndex = i;
                            break;
                        }
                    }
                    req.raycastTimer = 0;
                }

                if(req.rayPathIndex < len){
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
            }

            return req.done;
        }

        return false;
    }
    /** Starts or restarts the pathfinding thread. */
    private void start(){
        stop();
        thread = Threads.daemon("ControlPathfinder", this);
    }

    /** Stops the pathfinding thread. */
    private void stop(){
        if(thread != null){
            thread.interrupt();
            thread = null;
        }
        requests.clear();
    }

    private static boolean raycast(int type, int x1, int y1, int x2, int y2){
        int ww = world.width(), wh = world.height();
        int x = x1, dx = Math.abs(x2 - x), sx = x < x2 ? 1 : -1;
        int y = y1, dy = Math.abs(y2 - y), sy = y < y2 ? 1 : -1;
        int e2, err = dx - dy;

        while(x >= 0 && y >= 0 && x < ww && y < wh){
            if(avoid(type, x + y * wwidth)) return true;
            if(x == x2 && y == y2) return false;

            e2 = 2 * err;
            if(e2 > -dy){
                err -= dy;
                x += sx;
            }

            if(e2 < dx){
                err += dx;
                y += sy;
            }


            //no diagonals allowed here, mimics how units actually move
            /*
            if(2 * err + dy > dx - 2 * err){
                err -= dy;
                x += sx;
            }else{
                err += dx;
                y += sy;
            }*/

        }

        return true;
    }

    private static boolean permissiveRaycast(int type, int x1, int y1, int x2, int y2){
        int ww = world.width(), wh = world.height();
        int x = x1, dx = Math.abs(x2 - x), sx = x < x2 ? 1 : -1;
        int y = y1, dy = Math.abs(y2 - y), sy = y < y2 ? 1 : -1;
        int e2, err = dx - dy;

        while(x >= 0 && y >= 0 && x < ww && y < wh){
            if(solid(type, x + y * wwidth)) return true;
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

    static boolean cast(int pathType, int from, int to){
        return raycast(pathType, from % wwidth, from / wwidth, to % wwidth, to / wwidth);
    }

    private Tile tile(int pos){
        return world.tiles.geti(pos);
    }

    //distance heuristic: manhattan
    private static float heuristic(int a, int b){
        int x = a % wwidth, x2 = b % wwidth, y = a / wwidth, y2 = b / wwidth;
        return Math.abs(x - x2) + Math.abs(y - y2);
    }

    private static int cost(int type, int tilePos){
        return costTypes.items[type].getCost(null, pathfinder.tiles[tilePos]);
    }

    private static boolean avoid(int type, int tilePos){
        int cost = cost(type, tilePos);
        return cost == impassable || cost >= 2;
    }

    private static boolean solid(int type, int tilePos){
        return cost(type, tilePos) == impassable;
    }

    private static float tileCost(int type, int a, int b){
        //currently flat cost
        return cost(type, b);
    }

    @Override
    public void run(){
        while(true){
            //stop on client, no updating
            if(net.client()) return;
            try{
                if(state.isPlaying()){
                    queue.run();

                    //total update time no longer than maxUpdate
                    for(var req : threadRequests){
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

    //TODO each one of these could run in its own thread.
    static class PathRequest{
        volatile boolean done = false;
        volatile boolean foundEnd = false;

        final Vec2 lastPos = new Vec2();
        float stuckTimer = 0f;

        final Vec2 destination = new Vec2();
        final Vec2 lastDestination = new Vec2();

        volatile Unit unit;
        volatile int pathType;
        volatile int lastWorldUpdate;

        //TODO only access on main thread??
        volatile int pathIndex;

        int rayPathIndex = -1;
        IntSeq result = new IntSeq();
        float raycastTimer;

        PathfindQueue frontier = new PathfindQueue();
        //node index -> node it came from
        IntIntMap cameFrom = new IntIntMap();
        //node index -> total cost
        IntFloatMap costs = new IntFloatMap();

        int start, goal;

        long lastUpdateId;
        long lastTime;

        volatile int lastId, curId;

        void update(long maxUpdateNs){
            if(curId != lastId){
                clear();
            }
            lastId = curId;

            //re-do everything when world updates
            if(Time.timeSinceMillis(lastTime) > 1000 * 2 && (worldUpdateId != lastWorldUpdate || !destination.epsilonEquals(lastDestination, 2f))){
                lastTime = Time.millis();
                lastWorldUpdate = worldUpdateId;
                pathIndex = 0;
                rayPathIndex = -1;
                result.clear();
                clear();
            }

            if(done) return;

            long ns = Time.nanos();
            int counter = 0;
            //Log.info("running; @ in frontier", frontier.size);

            while(frontier.size > 0){
                int current = frontier.poll();

                if(current == goal){
                    foundEnd = true;
                    break;
                }

                int cx = current % wwidth, cy = current / wwidth;

                //TODO corner traps? d8 vs d4 here?
                for(Point2 point : Geometry.d4){
                    int newx = cx + point.x, newy = cy + point.y;
                    int next = newx + wwidth * newy;

                    if(newx >= wwidth || newy >= wheight || newx < 0 || newy < 0) continue;

                    if(cost(pathType, next) == impassable) continue;

                    float newCost = costs.get(current) + tileCost(pathType, current, next);
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
            result.clear();

            if(foundEnd){
                int cur = goal;
                while(cur != start){
                    result.add(cur);
                    cur = cameFrom.get(cur);
                }

                result.reverse();

                smoothPath();
            }

            //TODO free resources?

            done = true;
        }

        void smoothPath(){
            int len = result.size;
            if(len <= 2) return;

            int output = 1, input = 2;

            while(input < len){
                if(cast(pathType, result.get(output - 1), result.get(input))){
                    result.swap(output, input - 1);
                    output++;
                }
                input++;
            }

            result.swap(output, input - 1);
            result.size = output + 1;
        }

        void clear(){
            done = false;

            //TODO could be less expensive?
            frontier = new PathfindQueue(20);
            cameFrom.clear();
            costs.clear();

            start = world.packArray(unit.tileX(), unit.tileY());
            goal = world.packArray(World.toTile(destination.x), World.toTile(destination.y));

            cameFrom.put(start, start);
            costs.put(start, 0);

            frontier.add(start, 0);

            foundEnd = false;
            result.clear();
            lastDestination.set(destination);
        }
    }
}
