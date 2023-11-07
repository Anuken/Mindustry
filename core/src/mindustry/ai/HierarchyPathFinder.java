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
import mindustry.ui.*;

import static mindustry.Vars.*;
import static mindustry.ai.Pathfinder.*;

//https://webdocs.cs.ualberta.ca/~mmueller/ps/hpastar.pdf
//https://www.gameaipro.com/GameAIPro/GameAIPro_Chapter23_Crowd_Pathfinding_and_Steering_Using_Flow_Field_Tiles.pdf
public class HierarchyPathFinder{
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
    Cluster[][] clusters;
    int clusterSize = 12;

    int cwidth, cheight;

    //TODO: make thread-local (they are dereferenced rarely anyway)
    static PathfindQueue frontier = new PathfindQueue();
    //node index -> total cost
    static IntFloatMap costs = new IntFloatMap();
    //
    static IntSet usedEdges = new IntSet();
    static IntSeq bfsQueue = new IntSeq();
    static LongSeq tmpEdges = new LongSeq();
    //node index (NodeIndex struct) -> node it came from
    static IntIntMap cameFrom = new IntIntMap();

    public HierarchyPathFinder(){

        Events.on(WorldLoadEvent.class, event -> {
            //TODO 5 path costs, arbitrary number
            clusters = new Cluster[5][];
            clusterSize = 12; //TODO arbitrary
            cwidth = Mathf.ceil((float)world.width() / clusterSize);
            cheight = Mathf.ceil((float)world.height() / clusterSize);

            for(int cy = 0; cy < cwidth; cy++){
                for(int cx = 0; cx < cheight; cx++){
                    createCluster(Team.sharded.id, costGround, cx, cy);
                }
            }
        });

        //TODO very inefficient, this is only for debugging
        Events.on(TileChangeEvent.class, e -> {
            createCluster(Team.sharded.id, costGround, e.tile.x / clusterSize, e.tile.y / clusterSize);
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

                    Lines.stroke(3f);
                    Draw.color(Color.orange);
                    int node = findClosestNode(Team.sharded.id, 0, player.tileX(), player.tileY());
                    int dest = findClosestNode(Team.sharded.id, 0, World.toTile(Core.input.mouseWorldX()), World.toTile(Core.input.mouseWorldY()));
                    if(node != Integer.MAX_VALUE && dest != Integer.MAX_VALUE){
                        var result = clusterAstar(0, node, dest);
                        if(result != null){
                            for(int i = -1; i < result.size - 1; i++){
                                int current = i == -1 ? node : result.items[i], next = result.items[i + 1];
                                portalToVec(0, NodeIndex.cluster(current), NodeIndex.dir(current), NodeIndex.portal(current), Tmp.v1);
                                portalToVec(0, NodeIndex.cluster(next), NodeIndex.dir(next), NodeIndex.portal(next), Tmp.v2);
                                Lines.line(Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y);
                            }
                        }

                        nodeToVec(dest, Tmp.v1);
                        Fonts.outline.draw(clusterNodeHeuristic(0, node, dest) + "", Tmp.v1.x, Tmp.v1.y);
                    }

                    Draw.reset();
                });
            });
        }
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
        frontier.clear();
        costs.clear();

        //TODO: this can be faster and more memory efficient by making costs a NxN array... probably?
        costs.put(startPos, 0);
        frontier.add(startPos, 0);

        if(debug && false){
            Fx.debugLine.at(Point2.x(startPos) * tilesize, Point2.y(startPos) * tilesize, 0f, Color.purple,
            new Vec2[]{new Vec2(Point2.x(startPos), Point2.y(startPos)).scl(tilesize), new Vec2(Point2.x(goalPos), Point2.y(goalPos)).scl(tilesize)});
        }

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

    //uses BFS to find the closest node index to specified coordinates
    //this node is used in cluster A*
    /** @return MAX_VALUE if no node is found */
    private int findClosestNode(int team, int pathCost, int tileX, int tileY){
        int cx = tileX / clusterSize, cy = tileY / clusterSize;

        if(cx < 0 || cy < 0 || cx >= cwidth || cy >= cheight){
            return Integer.MAX_VALUE;
        }

        //TODO
        PathCost cost = ControlPathfinder.costGround;

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

    @Nullable IntSeq clusterAstar(int pathCost, int startNodeIndex, int endNodeIndex){
        var v1 = nodeToVec(startNodeIndex, Tmp.v1);
        var v2 = nodeToVec(endNodeIndex, Tmp.v2);
        Fx.placeBlock.at(v1.x, v1.y, 1);
        Fx.placeBlock.at(v2.x, v2.y, 1);

        if(startNodeIndex == endNodeIndex){
            //TODO alloc
            return IntSeq.with(startNodeIndex);
        }

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
                checkEdges(pathCost, current, endNodeIndex, cx, cy, innerCons);
            }

            //edges that this node 'faces' from the other side
            int nextCx = cx + Geometry.d4[dir].x, nextCy = cy + Geometry.d4[dir].y;
            if(nextCx >= 0 && nextCy >= 0 && nextCx < cwidth && nextCy < cheight){
                int nextClusteri = nextCx + nextCy * cwidth;
                Cluster nextCluster = clusters[pathCost][nextClusteri];
                int relativeDir = (dir + 2) % 4;
                LongSeq outerCons = nextCluster.portalConnections[relativeDir] == null ? null : nextCluster.portalConnections[relativeDir][portal];
                if(outerCons != null){
                    checkEdges(pathCost, current, endNodeIndex, nextCx, nextCy, outerCons);
                }
            }
        }

        if(foundEnd){
            IntSeq result = new IntSeq();

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

    void checkEdges(int pathCost, int current, int goal, int cx, int cy, LongSeq connections){
        for(int i = 0; i < connections.size; i++){
            long con = connections.items[i];
            float cost = IntraEdge.cost(con);
            int otherDir = IntraEdge.dir(con), otherPortal = IntraEdge.portal(con);
            int next = makeNodeIndex(cx, cy, otherDir, otherPortal);

            float newCost = costs.get(current) + cost;

            if(newCost < costs.get(next, Float.POSITIVE_INFINITY)){
                costs.put(next, newCost);

                frontier.add(next, newCost + clusterNodeHeuristic(pathCost, next, goal));
                cameFrom.put(next, current);

                //TODO debug
                line(nodeToVec(current, Tmp.v1), nodeToVec(next, Tmp.v2));
            }
        }
    }

    Cluster cluster(int pathCost, int cx, int cy){
        return clusters[pathCost][cx + cwidth * cy];
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
