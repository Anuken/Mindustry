package mindustry.ai;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

import static mindustry.Vars.*;
import static mindustry.ai.Pathfinder.*;

//https://webdocs.cs.ualberta.ca/~mmueller/ps/hpastar.pdf
//https://www.gameaipro.com/GameAIPro/GameAIPro_Chapter23_Crowd_Pathfinding_and_Steering_Using_Flow_Field_Tiles.pdf
public class HierarchyPathFinder implements Runnable{
    private static final long maxUpdate = Time.millisToNanos(12);
    private static final int updateFPS = 30;
    private static final int updateInterval = 1000 / updateFPS;

    static final int clusterSize = 12;

    static final boolean debug = true;

    static final int[] offsets = {
    1, 0, //right: bottom to top
    0, 1, //top: left to right
    0, 0, //left: bottom to top
    0, 0  //bottom: left to right
    };

    static final int[] moveDirs = {
    0, 1,
    1, 0,
    0, 1,
    1, 0
    };

    static final int[] nextOffsets = {
    1, 0,
    0, 1,
    -1, 0,
    0, -1
    };

    //maps pathCost -> flattened array of clusters in 2D
    //(what about teams? different path costs?)
    Cluster[][] clusters;

    int cwidth, cheight;

    //temporarily used for resolving connections for intra-edges
    IntSet usedEdges = new IntSet();
    //tasks to run on pathfinding thread
    TaskQueue queue = new TaskQueue();
    //individual requests based on unit
    ObjectMap<Unit, PathRequest> unitRequests = new ObjectMap<>();
    //maps position in world in (x + y * width format) to a cache of flow fields
    IntMap<FieldCache> fields = new IntMap<>();


    //these are for inner edge A*
    IntFloatMap innerCosts = new IntFloatMap();
    PathfindQueue innerFrontier = new PathfindQueue();

    //ONLY modify on pathfinding thread.
    IntSet clustersToUpdate = new IntSet();
    IntSet clustersToInnerUpdate = new IntSet();

    /** Current pathfinding thread */
    @Nullable Thread thread;

    //path requests are per-unit
    //these contain
    static class PathRequest{
        final Unit unit;
        final int destination;
        //resulting path of nodes
        final IntSeq resultPath = new IntSeq();
        //node index -> total cost
        final IntFloatMap costs = new IntFloatMap();
        //node index (NodeIndex struct) -> node it came from TODO merge them
        final IntIntMap cameFrom = new IntIntMap();
        //frontier for A*
        final PathfindQueue frontier = new PathfindQueue();

        //main thread only!
        long lastUpdateId;

        public PathRequest(Unit unit, int destination){
            this.unit = unit;
            this.destination = destination;
        }
    }

    static class FieldCache{
        final PathCost cost;
        final int team;
        final int goalPos;
        //frontier for flow fields
        final IntQueue frontier = new IntQueue();
        //maps cluster index to field weights; 0 means uninitialized
        final IntMap<int[]> fields = new IntMap<>();

        //TODO: node map for merging
        //TODO: how to extend flowfields?


        public FieldCache(PathCost cost, int team, int goalPos){
            this.cost = cost;
            this.team = team;
            this.goalPos = goalPos;
        }
    }

    public HierarchyPathFinder(){

        Events.on(ResetEvent.class, event -> stop());

        Events.on(WorldLoadEvent.class, event -> {
            stop();

            //TODO 5 path costs, arbitrary number
            clusters = new Cluster[5][];
            cwidth = Mathf.ceil((float)world.width() / clusterSize);
            cheight = Mathf.ceil((float)world.height() / clusterSize);

            start();
        });

        //TODO very inefficient, this is only for debugging
        Events.on(TileChangeEvent.class, e -> {

            e.tile.getLinkedTiles(t -> {
                int x = t.x, y = t.y, mx = x % clusterSize, my = y % clusterSize, cx = x / clusterSize, cy = y / clusterSize, cluster = cx + cy * cwidth;

                //is at the edge of a cluster; this means the portals may have changed.
                if(mx == 0 || my == 0 || mx == clusterSize - 1 || my == clusterSize - 1){
                    queue.post(() -> clustersToUpdate.add(cluster));
                }else{
                    //there is no need to recompute portals for block updates that are not on the edge.
                    queue.post(() -> clustersToInnerUpdate.add(cluster));
                }
            });

            //TODO: if near center of cluster:
            //- re-do inner A* only
            //- otherwise, re-do everything

            //TODO: recalculate affected flow fields? or just all of them?
        });

        //invalidate paths
        Events.run(Trigger.update, () -> {
            for(var req : unitRequests.values()){
                //skipped N update -> drop it
                if(req.lastUpdateId <= state.updateId - 10){
                    //concurrent modification!
                    Core.app.post(() -> unitRequests.remove(req.unit));
                }
            }
        });

        if(debug){
            Events.run(Trigger.draw, () -> {
                int team = Team.sharded.id;
                int cost = costGround;

                if(clusters == null || clusters[cost] == null) return;

                Draw.draw(Layer.overlayUI, () -> {
                    Lines.stroke(1f);
                    for(int cx = 0; cx < cwidth; cx++){
                        for(int cy = 0; cy < cheight; cy++){
                            var cluster = clusters[cost][cy * cwidth + cx];
                            if(cluster != null){
                                Lines.stroke(0.5f);
                                Draw.color(Color.gray);
                                Lines.stroke(1f);

                                Lines.rect(cx * clusterSize * tilesize - tilesize/2f, cy * clusterSize * tilesize - tilesize/2f, clusterSize * tilesize, clusterSize * tilesize);


                                for(int d = 0; d < 4; d++){
                                    IntSeq portals = cluster.portals[d];
                                    if(portals != null){

                                        for(int i = 0; i < portals.size; i++){
                                            int pos = portals.items[i];
                                            int from = Point2.x(pos), to = Point2.y(pos);
                                            float width = tilesize * (Math.abs(from - to) + 1), height = tilesize;

                                            portalToVec(cluster, cx, cy, d, i, Tmp.v1);

                                            Draw.color(Color.brown);
                                            Lines.ellipse(30, Tmp.v1.x, Tmp.v1.y, width / 2f, height / 2f, d * 90f - 90f);

                                            LongSeq connections = cluster.portalConnections[d] == null ? null : cluster.portalConnections[d][i];

                                            if(connections != null){
                                                Draw.color(Color.forest);
                                                for(int coni = 0; coni < connections.size; coni ++){
                                                    long con = connections.items[coni];

                                                    portalToVec(cluster, cx, cy, IntraEdge.dir(con), IntraEdge.portal(con), Tmp.v2);

                                                    float
                                                    x1 = Tmp.v1.x, y1 = Tmp.v1.y,
                                                    x2 = Tmp.v2.x, y2 = Tmp.v2.y,
                                                    mx = (cx * clusterSize + clusterSize / 2f) * tilesize, my = (cy * clusterSize + clusterSize / 2f) * tilesize;
                                                    //Lines.curve(x1, y1, mx, my, mx, my, x2, y2, 20);
                                                    Lines.line(x1, y1, x2, y2);

                                                }
                                            }
                                        }
                                    }
                                }

                                //TODO draw connections.

                                /*
                                Draw.color(Color.magenta);
                                for(var con : cluster.cons){
                                    float
                                    x1 = Point2.x(con.posFrom) * tilesize, y1 = Point2.y(con.posFrom) * tilesize,
                                    x2 = Point2.x(con.posTo) * tilesize, y2 = Point2.y(con.posTo) * tilesize,
                                    mx = (cx * clusterSize + clusterSize/2f) * tilesize, my = (cy * clusterSize + clusterSize/2f) * tilesize;
                                    //Lines.curve(x1, y1, mx, my, mx, my, x2, y2, 20);
                                    Lines.line(x1, y1, x2, y2);
                                }*/
                            }
                        }
                    }

                    /*
                    if(fields != null){
                        for(var entry : fields){
                            int cx = entry.key % cwidth, cy = entry.key / cwidth;
                            for(int y = 0; y < clusterSize; y++){
                                for(int x = 0; x < clusterSize; x++){
                                    int value = entry.value[x + y * clusterSize];
                                    Tmp.c1.a = 1f;
                                    Lines.stroke(0.8f, Tmp.c1.fromHsv(value * 3f, 1f, 1f));
                                    Draw.alpha(0.5f);
                                    Fill.square((x + cx * clusterSize) * tilesize, (y + cy * clusterSize) * tilesize, tilesize/2f);
                                }
                            }
                        }
                    }
                    */


                });
            });
        }
    }

    /** Starts or restarts the pathfinding thread. */
    private void start(){
        stop();
        if(net.client()) return;

        thread = new Thread(this, "Control Pathfinder");
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    /** Stops the pathfinding thread. */
    private void stop(){
        if(thread != null){
            thread.interrupt();
            thread = null;
        }
        queue.clear();
    }

    Vec2 nodeToVec(int current, Vec2 out){
        portalToVec(0, NodeIndex.cluster(current), NodeIndex.dir(current), NodeIndex.portal(current), out);
        return out;
    }

    void portalToVec(int pathCost, int cluster, int direction, int portalIndex, Vec2 out){
        portalToVec(clusters[pathCost][cluster], cluster % cwidth, cluster / cwidth, direction, portalIndex, out);
    }

    void portalToVec(Cluster cluster, int cx, int cy, int direction, int portalIndex, Vec2 out){
        int pos = cluster.portals[direction].items[portalIndex];
        int from = Point2.x(pos), to = Point2.y(pos);
        int addX = moveDirs[direction * 2], addY = moveDirs[direction * 2 + 1];
        float average = (from + to) / 2f;

        float
        x = (addX * average + cx * clusterSize + offsets[direction * 2] * (clusterSize - 1) + nextOffsets[direction * 2] / 2f) * tilesize,
        y = (addY * average + cy * clusterSize + offsets[direction * 2 + 1] * (clusterSize - 1) + nextOffsets[direction * 2 + 1] / 2f) * tilesize;

        out.set(x, y);
    }

    //TODO: this is never called yet. should be invoked during pathfinding
    void createCluster(int team, int pathCost, int cx, int cy){
        if(clusters[pathCost] == null) clusters[pathCost] = new Cluster[cwidth * cheight];
        Cluster cluster = clusters[pathCost][cy * cwidth + cx];
        if(cluster == null){
            cluster = clusters[pathCost][cy * cwidth + cx] = new Cluster();
        }else{
            //reset data
            for(var p : cluster.portals){
                p.clear();
            }
        }

        //clear all connections, since portals changed, they need to be recomputed.
        cluster.portalConnections = new LongSeq[4][];

        //TODO: other cluster inner edges should be recomputed if changed.

        //TODO look it up based on number.
        PathCost cost = ControlPathfinder.costGround;

        for(int direction = 0; direction < 4; direction++){
            int otherX = cx + Geometry.d4x(direction), otherY = cy + Geometry.d4y(direction);
            //out of bounds, no portals in this direction
            if(otherX < 0 || otherY < 0 || otherX >= cwidth || otherY >= cheight){
                continue;
            }

            Cluster other = clusters[pathCost][otherX + otherY * cwidth];
            IntSeq portals;

            if(other == null){
                //create new portals at direction
                portals = cluster.portals[direction] = new IntSeq(4);
            }else{
                //share portals with the other cluster
                portals = cluster.portals[direction] = other.portals[(direction + 2) % 4];

                //clear the portals, they're being recalculated now
                portals.clear();
            }

            int addX = moveDirs[direction * 2], addY = moveDirs[direction * 2 + 1];
            int
            baseX = cx * clusterSize + offsets[direction * 2] * (clusterSize - 1),
            baseY = cy * clusterSize + offsets[direction * 2 + 1] * (clusterSize - 1),
            nextBaseX = baseX + Geometry.d4[direction].x,
            nextBaseY = baseY + Geometry.d4[direction].y;

            int lastPortal = -1;
            boolean prevSolid = true;

            for(int i = 0; i < clusterSize; i++){
                int x = baseX + addX * i, y = baseY + addY * i;

                //scan for portals
                if(solid(team, cost, x, y) || solid(team, cost, nextBaseX + addX * i, nextBaseY + addY * i)){
                    int previous = i - 1;
                    //hit a wall, create portals between the two points
                    if(!prevSolid && previous >= lastPortal){
                        //portals are an inclusive range
                        portals.add(Point2.pack(previous, lastPortal));
                    }
                    prevSolid = true;
                }else{
                    //empty area encountered, mark the location of portal start
                    if(prevSolid){
                        lastPortal = i;
                    }
                    prevSolid = false;
                }
            }

            //at the end of the loop, close any un-initialized portals; this is copy pasted code
            int previous = clusterSize - 1;
            if(!prevSolid && previous >= lastPortal){
                //portals are an inclusive range
                portals.add(Point2.pack(previous, lastPortal));
            }
        }

        connectInnerEdges(cx, cy, team, cost, cluster);
    }

    void connectInnerEdges(int cx, int cy, int team, PathCost cost, Cluster cluster){
        int minX = cx * clusterSize, minY = cy * clusterSize, maxX = Math.min(minX + clusterSize - 1, wwidth - 1), maxY = Math.min(minY + clusterSize - 1, wheight - 1);
        
        usedEdges.clear();

        //TODO: how the hell to identify a vertex?
        //cluster (i16) | direction (i2) | index (i14)

        //TODO: clear portal connections

        for(int direction = 0; direction < 4; direction++){
            var portals = cluster.portals[direction];
            if(portals == null) continue;

            int addX = moveDirs[direction * 2], addY = moveDirs[direction * 2 + 1];

            for(int i = 0; i < portals.size; i++){
                usedEdges.add(Point2.pack(direction, i));
                
                int
                portal = portals.items[i],
                from = Point2.x(portal), to = Point2.y(portal),
                average = (from + to) / 2,
                x = (addX * average + cx * clusterSize + offsets[direction * 2] * (clusterSize - 1)),
                y = (addY * average + cy * clusterSize + offsets[direction * 2 + 1] * (clusterSize - 1));

                for(int otherDir = 0; otherDir < 4; otherDir++){
                    var otherPortals = cluster.portals[otherDir];
                    if(otherPortals == null) continue;

                    for(int j = 0; j < otherPortals.size; j++){

                        //TODO redundant calculations?
                        if(!usedEdges.contains(Point2.pack(otherDir, j))){

                            int
                            other = otherPortals.items[j],
                            otherFrom = Point2.x(other), otherTo = Point2.y(other),
                            otherAverage = (otherFrom + otherTo) / 2,
                            ox = cx * clusterSize + offsets[otherDir * 2] * (clusterSize - 1),
                            oy = cy * clusterSize + offsets[otherDir * 2 + 1] * (clusterSize - 1),
                            otherX = (moveDirs[otherDir * 2] * otherAverage + ox),
                            otherY = (moveDirs[otherDir * 2 + 1] * otherAverage + oy);

                            //duplicate portal; should never happen.
                            if(Point2.pack(x, y) == Point2.pack(otherX, otherY)){
                                continue;
                            }

                            float connectionCost = innerAstar(
                                team, cost,
                                minX, minY, maxX, maxY,
                                x + y * wwidth,
                                otherX + otherY * wwidth,
                                (moveDirs[otherDir * 2] * otherFrom + ox),
                                (moveDirs[otherDir * 2 + 1] * otherFrom + oy),
                                (moveDirs[otherDir * 2] * otherTo + ox),
                                (moveDirs[otherDir * 2 + 1] * otherTo + oy)
                            );

                            if(connectionCost != -1f){
                                if(cluster.portalConnections[direction] == null) cluster.portalConnections[direction] = new LongSeq[cluster.portals[direction].size];
                                if(cluster.portalConnections[otherDir] == null) cluster.portalConnections[otherDir] = new LongSeq[cluster.portals[otherDir].size];
                                if(cluster.portalConnections[direction][i] == null) cluster.portalConnections[direction][i] = new LongSeq(8);
                                if(cluster.portalConnections[otherDir][j] == null) cluster.portalConnections[otherDir][j] = new LongSeq(8);

                                //TODO: can there be duplicate edges??
                                cluster.portalConnections[direction][i].add(IntraEdge.get(otherDir, j, connectionCost));
                                cluster.portalConnections[otherDir][j].add(IntraEdge.get(direction, i, connectionCost));
                            }
                        }
                    }
                }
            }
        }
    }

    //distance heuristic: manhattan
    private static float heuristic(int a, int b){
        int x = a % wwidth, x2 = b % wwidth, y = a / wwidth, y2 = b / wwidth;
        return Math.abs(x - x2) + Math.abs(y - y2);
    }

    private static int tcost(int team, PathCost cost, int tilePos){
        return cost.getCost(team, pathfinder.tiles[tilePos]);
    }

    private static float tileCost(int team, PathCost type, int a, int b){
        //currently flat cost
        return cost(team, type, b);
    }

    /** @return -1 if no path was found */
    float innerAstar(int team, PathCost cost, int minX, int minY, int maxX, int maxY, int startPos, int goalPos, int goalX1, int goalY1, int goalX2, int goalY2){
        var frontier = innerFrontier;
        var costs = innerCosts;

        frontier.clear();
        costs.clear();

        //TODO: this can be faster and more memory efficient by making costs a NxN array... probably?
        costs.put(startPos, 0);
        frontier.add(startPos, 0);

        while(frontier.size > 0){
            int current = frontier.poll();

            int cx = current % wwidth, cy = current / wwidth;

            //found the goal (it's in the portal rectangle)
            //TODO portal rectangle approach does not work.
            if((cx >= goalX1 && cy >= goalY1 && cx <= goalX2 && cy <= goalY2) || current == goalPos){
                return costs.get(current);
            }

            for(Point2 point : Geometry.d4){
                int newx = cx + point.x, newy = cy + point.y;
                int next = newx + wwidth * newy;

                if(newx > maxX || newy > maxY || newx < minX || newy < minY) continue;

                //TODO fallback mode for enemy walls or whatever
                if(tcost(team, cost, next) == impassable) continue;

                float add = tileCost(team, cost, current, next);

                if(add < 0) continue;

                float newCost = costs.get(current) + add;

                if(newCost < costs.get(next, Float.POSITIVE_INFINITY)){
                    costs.put(next, newCost);
                    float priority = newCost + heuristic(next, goalPos);
                    frontier.add(next, priority);
                }
            }
        }

        return -1f;
    }

    int makeNodeIndex(int cx, int cy, int dir, int portal){
        //to make sure there's only one way to refer to each node, the direction must be 0 or 1 (referring to portals on the top or right edge)

        //direction can only be 2 if cluster X is 0 (left edge of map)
        if(dir == 2 && cx != 0){
            dir = 0;
            cx --;
        }

        //direction can only be 3 if cluster Y is 0 (bottom edge of map)
        if(dir == 3 && cy != 0){
            dir = 1;
            cy --;
        }

        return NodeIndex.get(cx + cy * cwidth, dir, portal);
    }

    //uses A* to find the closest node index to specified coordinates
    //this node is used in cluster A*
    /** @return MAX_VALUE if no node is found */
    private int findClosestNode(int team, int pathCost, int tileX, int tileY){
        int cx = tileX / clusterSize, cy = tileY / clusterSize;

        if(cx < 0 || cy < 0 || cx >= cwidth || cy >= cheight){
            return Integer.MAX_VALUE;
        }

        //TODO
        PathCost cost = ControlPathfinder.costGround;

        //TODO: cluster can be null!!
        Cluster cluster = clusters[pathCost][cx + cy * cwidth];
        int minX = cx * clusterSize, minY = cy * clusterSize, maxX = Math.min(minX + clusterSize - 1, wwidth - 1), maxY = Math.min(minY + clusterSize - 1, wheight - 1);

        int bestPortalPair = Integer.MAX_VALUE;
        float bestCost = Float.MAX_VALUE;

        if(cluster != null){ //TODO create on demand??

            //A* to every node, find the best one (I know there's a better algorithm for this, probably dijkstra)
            for(int dir = 0; dir < 4; dir++){
                var portals = cluster.portals[dir];
                if(portals == null) continue;

                for(int j = 0; j < portals.size; j++){

                    int
                    other = portals.items[j],
                    otherFrom = Point2.x(other), otherTo = Point2.y(other),
                    otherAverage = (otherFrom + otherTo) / 2,
                    ox = cx * clusterSize + offsets[dir * 2] * (clusterSize - 1),
                    oy = cy * clusterSize + offsets[dir * 2 + 1] * (clusterSize - 1),
                    otherX = (moveDirs[dir * 2] * otherAverage + ox),
                    otherY = (moveDirs[dir * 2 + 1] * otherAverage + oy);

                    float connectionCost = innerAstar(
                    team, cost,
                    minX, minY, maxX, maxY,
                    tileX + tileY * wwidth,
                    otherX + otherY * wwidth,
                    //TODO these are wrong and never actually trigger
                    (moveDirs[dir * 2] * otherFrom + ox),
                    (moveDirs[dir * 2 + 1] * otherFrom + oy),
                    (moveDirs[dir * 2] * otherTo + ox),
                    (moveDirs[dir * 2 + 1] * otherTo + oy)
                    );

                    //better cost found, update and return
                    if(connectionCost != -1f && connectionCost < bestCost){
                        bestPortalPair = Point2.pack(dir, j);
                        bestCost = connectionCost;
                    }
                }
            }

            if(bestPortalPair != Integer.MAX_VALUE){
                return makeNodeIndex(cx, cy, Point2.x(bestPortalPair), Point2.y(bestPortalPair));
            }
        }

        return Integer.MAX_VALUE;
    }

    //distance heuristic: manhattan
    private float clusterNodeHeuristic(int pathCost, int nodeA, int nodeB){
        int
        clusterA = NodeIndex.cluster(nodeA),
        dirA = NodeIndex.dir(nodeA),
        portalA = NodeIndex.portal(nodeA),
        clusterB = NodeIndex.cluster(nodeB),
        dirB = NodeIndex.dir(nodeB),
        portalB = NodeIndex.portal(nodeB),
        rangeA = clusters[pathCost][clusterA].portals[dirA].items[portalA],
        rangeB = clusters[pathCost][clusterB].portals[dirB].items[portalB];

        float
        averageA = (Point2.x(rangeA) + Point2.y(rangeA)) / 2f,
        x1 = (moveDirs[dirA * 2] * averageA + (clusterA % cwidth) * clusterSize + offsets[dirA * 2] * (clusterSize - 1) + nextOffsets[dirA * 2] / 2f),
        y1 = (moveDirs[dirA * 2 + 1] * averageA + (clusterA / cwidth) * clusterSize + offsets[dirA * 2 + 1] * (clusterSize - 1) + nextOffsets[dirA * 2 + 1] / 2f),

        averageB = (Point2.x(rangeB) + Point2.y(rangeB)) / 2f,
        x2 = (moveDirs[dirB * 2] * averageB + (clusterB % cwidth) * clusterSize + offsets[dirB * 2] * (clusterSize - 1) + nextOffsets[dirB * 2] / 2f),
        y2 = (moveDirs[dirB * 2 + 1] * averageB + (clusterB / cwidth) * clusterSize + offsets[dirB * 2 + 1] * (clusterSize - 1) + nextOffsets[dirB * 2 + 1] / 2f);

        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    @Nullable IntSeq clusterAstar(PathRequest request, int pathCost, int startNodeIndex, int endNodeIndex){
        var result = request.resultPath;

        if(startNodeIndex == endNodeIndex){
            result.clear();
            result.add(startNodeIndex);
            //TODO alloc
            return result;
        }

        var costs = request.costs;
        var cameFrom = request.cameFrom;
        var frontier = request.frontier;

        frontier.clear();
        costs.clear();
        cameFrom.clear();

        cameFrom.put(startNodeIndex, startNodeIndex);
        costs.put(startNodeIndex, 0);
        frontier.add(startNodeIndex, 0);

        boolean foundEnd = false;

        while(frontier.size > 0){
            int current = frontier.poll();

            if(current == endNodeIndex){
                foundEnd = true;
                break;
            }

            int cluster = NodeIndex.cluster(current), dir = NodeIndex.dir(current), portal = NodeIndex.portal(current);
            int cx = cluster % cwidth, cy = cluster / cwidth;
            Cluster clust = clusters[pathCost][cluster];
            LongSeq innerCons = clust.portalConnections[dir] == null || portal >= clust.portalConnections[dir].length ? null : clust.portalConnections[dir][portal];

            //edges for the cluster the node is 'in'
            if(innerCons != null){
                checkEdges(request, pathCost, current, endNodeIndex, cx, cy, innerCons);
            }

            //edges that this node 'faces' from the other side
            int nextCx = cx + Geometry.d4[dir].x, nextCy = cy + Geometry.d4[dir].y;
            if(nextCx >= 0 && nextCy >= 0 && nextCx < cwidth && nextCy < cheight){
                int nextClusteri = nextCx + nextCy * cwidth;
                Cluster nextCluster = clusters[pathCost][nextClusteri];
                int relativeDir = (dir + 2) % 4;
                LongSeq outerCons = nextCluster.portalConnections[relativeDir] == null ? null : nextCluster.portalConnections[relativeDir][portal];
                if(outerCons != null){
                    checkEdges(request, pathCost, current, endNodeIndex, nextCx, nextCy, outerCons);
                }
            }
        }

        if(foundEnd){
            result.clear();

            int cur = endNodeIndex;
            while(cur != startNodeIndex){
                result.add(cur);
                cur = cameFrom.get(cur);
            }

            result.reverse();

            return result;
        }
        return null;
    }

    static void line(Vec2 a, Vec2 b){
        Fx.debugLine.at(a.x, a.y, 0f, Color.blue.cpy().a(0.1f), new Vec2[]{a.cpy(), b.cpy()});
    }

    static void line(Vec2 a, Vec2 b, Color color){
        Fx.debugLine.at(a.x, a.y, 0f, color, new Vec2[]{a.cpy(), b.cpy()});
    }

    void checkEdges(PathRequest request, int pathCost, int current, int goal, int cx, int cy, LongSeq connections){
        for(int i = 0; i < connections.size; i++){
            long con = connections.items[i];
            float cost = IntraEdge.cost(con);
            int otherDir = IntraEdge.dir(con), otherPortal = IntraEdge.portal(con);
            int next = makeNodeIndex(cx, cy, otherDir, otherPortal);

            float newCost = request.costs.get(current) + cost;

            if(newCost < request.costs.get(next, Float.POSITIVE_INFINITY)){
                request.costs.put(next, newCost);

                request.frontier.add(next, newCost + clusterNodeHeuristic(pathCost, next, goal));
                request.cameFrom.put(next, current);

                //TODO debug
                line(nodeToVec(current, Tmp.v1), nodeToVec(next, Tmp.v2));
            }
        }
    }

    private void updateFields(FieldCache cache, long nsToRun){
        var frontier = cache.frontier;
        var fields = cache.fields;
        var goalPos = cache.goalPos;
        var pcost = cache.cost;
        var team = cache.team;

        long start = Time.nanos();
        int counter = 0;

        //actually do the flow field part
        //TODO spread this out across many frames
        while(frontier.size > 0){
            int tile = frontier.removeLast();
            int baseX = tile % wwidth, baseY = tile / wwidth;
            int curWeightIndex = (baseX / clusterSize) + (baseY / clusterSize) * cwidth;
            int[] curWeights = fields.get(curWeightIndex);

            int cost = curWeights[baseX % clusterSize + ((baseY % clusterSize) * clusterSize)];

            if(cost != impassable){
                for(Point2 point : Geometry.d4){

                    int
                    dx = baseX + point.x, dy = baseY + point.y,
                    clx = dx / clusterSize, cly = dy / clusterSize;

                    if(clx < 0 || cly < 0 || dx >= wwidth || dy >= wheight) continue;

                    int nextWeightIndex = clx + cly * cwidth;

                    int[] weights = nextWeightIndex == curWeightIndex ? curWeights : fields.get(nextWeightIndex);

                    //out of bounds; not allowed to move this way because no weights were registered here
                    if(weights == null) continue;

                    int newPos = tile + point.x + point.y * wwidth;

                    //can't move back to the goal
                    if(newPos == goalPos) continue;

                    int newPosArray = (dx - clx * clusterSize) + (dy - cly * clusterSize) * clusterSize;
                    int otherCost = pcost.getCost(team, pathfinder.tiles[newPos]);
                    int oldCost = weights[newPosArray];

                    //a cost of 0 means uninitialized, OR it means we're at the goal position, but that's handled above
                    if((oldCost == 0 || oldCost > cost + otherCost) && otherCost != impassable){
                        frontier.addFirst(newPos);
                        weights[newPosArray] = cost + otherCost;
                    }
                }
            }

            //every N iterations, check the time spent - this prevents extra calls to nano time, which itself is slow
            if(nsToRun >= 0 && (counter++) >= 200){
                counter = 0;
                if(Time.timeSinceNanos(start) >= nsToRun){
                    return;
                }
            }
        }
    }

    public void initializePathRequest(PathRequest request, int team, int unitX, int unitY, int goalX, int goalY){
        int costId = 0;
        PathCost pcost = ControlPathfinder.costGround;

        int goalPos = (goalX + goalY * wwidth);

        int node = findClosestNode(team, costId, unitX, unitY);
        int dest = findClosestNode(team, costId, goalX, goalY);

        var nodePath = clusterAstar(request, costId, node, dest);

        //TODO: how to reuse
        FieldCache cache = this.fields.get(goalPos, () -> new FieldCache(pcost, team, goalPos));

        if(cache.frontier.isEmpty()){
            cache.frontier.addFirst(goalPos);
        }

        if(nodePath != null){

            int fsize = clusterSize * clusterSize;
            int cx = unitX / clusterSize, cy = unitY / clusterSize;

            var fields = cache.fields;

            fields.put(cx + cy * cwidth, new int[fsize]);

            for(int i = -1; i < nodePath.size; i++){
                int
                current = i == -1 ? node : nodePath.items[i],
                cluster = NodeIndex.cluster(current),
                dir = NodeIndex.dir(current),
                dx = Geometry.d4[dir].x,
                dy = Geometry.d4[dir].y,
                ox = cluster % cwidth + dx,
                oy = cluster / cwidth + dy;

                //store current cluster in the path list
                if(!fields.containsKey(cluster)){
                    fields.put(cluster, new int[fsize]);
                }

                //store directionals TODO out of bounds
                for(Point2 p : Geometry.d4){
                    int other = cluster + p.x + p.y * cwidth;
                    if(!fields.containsKey(other)){
                        fields.put(other, new int[fsize]);
                    }
                }

                //store directional/flipped version of cluster
                if(ox >= 0 && oy >= 0 && ox < cwidth && oy < cheight){
                    int other = ox + oy * cwidth;
                    if(!fields.containsKey(other)){
                        fields.put(other, new int[fsize]);
                    }

                    //store directionals again
                    for(Point2 p : Geometry.d4){
                        int other2 = other + p.x + p.y * cwidth;
                        if(!fields.containsKey(other2)){
                            fields.put(other2, new int[fsize]);
                        }
                    }
                }
            }
        }

    }

    public boolean getPathPosition(Unit unit, int pathId, Vec2 destination, Vec2 out, boolean[] noResultFound){
        int costId = 0;

        PathRequest request = unitRequests.get(unit);
        int
        destX = World.toTile(destination.x),
        destY = World.toTile(destination.y) * wwidth,
        destPos = destX + destY * wwidth;

        //TODO: collect old requests that have not been accessed in a while. not sure where.
        request.lastUpdateId = state.updateId;

        //use existing request if it exists.
        if(request != null && request.destination == destPos){

            Tile tileOn = unit.tileOn();
            //TODO: should fields be accessible from this thread?
            FieldCache fieldCache = fields.get(destPos);

            if(tileOn != null && fieldCache != null){
                int value = getCost(fieldCache.fields, tileOn.x, tileOn.y);

                Tile current = null;
                int tl = 0;
                //TODO: use raycasting and iterate on this for N steps
                for(Point2 point : Geometry.d8){
                    int dx = tileOn.x + point.x, dy = tileOn.y + point.y;

                    Tile other = world.tile(dx, dy);

                    if(other == null) continue;

                    int packed = world.packArray(dx, dy);
                    int otherCost = getCost(fieldCache.fields, dx, dy);

                    if(otherCost < value && (current == null || otherCost < tl) && passable(ControlPathfinder.costGround, unit.team.id, packed) &&
                    !(point.x != 0 && point.y != 0 && (!passable(ControlPathfinder.costGround, unit.team.id, world.packArray(tileOn.x + point.x, tileOn.y)) ||
                    (!passable(ControlPathfinder.costGround, unit.team.id, world.packArray(tileOn.x, tileOn.y + point.y)))))){ //diagonal corner trap

                        current = other;
                        tl = otherCost;
                    }
                }

                if(!(current == null || tl == impassable || (costId == costGround && current.dangerous() && !tileOn.dangerous()))){
                    out.set(current);
                    return true;
                }
            }

        }else{
            //queue new request.
            unitRequests.put(unit, request = new PathRequest(unit, destPos));

            PathRequest f = request;

            //on the pathfinding thread: initialize the request, meaning
            queue.post(() -> {
                initializePathRequest(f, unit.team.id, unit.tileX(), unit.tileY(),  destX, destY);
            });
        }

        noResultFound[0] = true;
        return false;
    }

    private int getCost(IntMap<int[]> fields, int x, int y){
        int[] field = fields.get(x / clusterSize + (y / clusterSize) * cwidth);
        if(field == null){
            return -1;
        }
        return field[(x % clusterSize) + (y % clusterSize) * clusterSize];
    }

    private static boolean passable(PathCost cost, int team, int pos){
        int amount = cost.getCost(team, pathfinder.tiles[pos]);
        //edge case: naval reports costs of 6000+ for non-liquids, even though they are not technically passable
        return amount != impassable && !(cost == costTypes.get(costNaval) && amount >= 6000);
    }

    private static boolean solid(int team, PathCost type, int x, int y){
        return x < 0 || y < 0 || x >= wwidth || y >= wheight || solid(team, type, x + y * wwidth, true);
    }

    private static boolean solid(int team, PathCost type, int tilePos, boolean checkWall){
        int cost = cost(team, type, tilePos);
        return cost == impassable || (checkWall && cost >= 6000);
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

    @Override
    public void run(){
        while(true){
            if(net.client()) return;
            try{

                if(state.isPlaying()){
                    queue.run();

                    clustersToUpdate.each(cluster -> {

                        //just in case: don't redundantly update inner clusters after you've recalculated it entirely
                        clustersToInnerUpdate.remove(cluster);
                    });

                    clustersToInnerUpdate.each(cluster -> {

                        //only recompute the inner links
                    });

                    clustersToInnerUpdate.clear();
                    clustersToUpdate.clear();

                    //TODO: update everything else too

                    //each update time (not total!) no longer than maxUpdate
                    for(FieldCache cache : fields.values()){
                        updateFields(cache, maxUpdate);
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

    static class Cluster{
        IntSeq[] portals = new IntSeq[4];
        //maps rotation + index of portal to list of IntraEdge objects
        LongSeq[][] portalConnections = new LongSeq[4][];
    }

    @Struct
    static class IntraEdgeStruct{
        @StructField(8)
        int dir;
        @StructField(8)
        int portal;

        float cost;
    }

    @Struct
    static class NodeIndexStruct{
        @StructField(22)
        int cluster;
        @StructField(2)
        int dir;
        @StructField(8)
        int portal;
    }
}
