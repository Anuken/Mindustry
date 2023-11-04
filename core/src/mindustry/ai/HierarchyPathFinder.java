package mindustry.ai;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.graphics.*;

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

    public HierarchyPathFinder(){

        Events.on(WorldLoadEvent.class, event -> {
            //TODO 5 path costs, arbitrary number
            clusters = new Cluster[5][];
            clusterSize = 12; //TODO arbitrary
            cwidth = Mathf.ceil((float)world.width() / clusterSize);
            cheight = Mathf.ceil((float)world.height() / clusterSize);

            for(int cx = 0; cx < cwidth; cx++){
                for(int cy = 0; cy < cheight; cy++){
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
                                Draw.color(Color.green);

                                Lines.rect(cx * clusterSize * tilesize - tilesize/2f, cy * clusterSize * tilesize - tilesize/2f, clusterSize * tilesize, clusterSize * tilesize);
                                Draw.color(Color.red);

                                for(int d = 0; d < 4; d++){
                                    IntSeq portals = cluster.portals[d];
                                    if(portals != null){
                                        int addX = moveDirs[d * 2], addY = moveDirs[d * 2 + 1];

                                        for(int i = 0; i < portals.size; i++){
                                            int pos = portals.items[i];
                                            int from = Point2.x(pos), to = Point2.y(pos);
                                            float width = tilesize * (Math.abs(from - to) + 1), height = tilesize;

                                            float average = (from + to) / 2f;

                                            float
                                            x = (addX * average + cx * clusterSize + offsets[d * 2] * (clusterSize - 1) + nextOffsets[d * 2] / 2f) * tilesize,
                                            y = (addY * average + cy * clusterSize + offsets[d * 2 + 1] * (clusterSize - 1) + nextOffsets[d * 2 + 1]/2f) * tilesize;

                                            Lines.ellipse(30, x, y, width / 2f, height / 2f, d * 90f - 90f);
                                        }
                                    }
                                }

                                Draw.color(Color.magenta);
                                for(var con : cluster.cons){
                                    float
                                    x1 = Point2.x(con.posFrom) * tilesize, y1 = Point2.y(con.posFrom) * tilesize,
                                    x2 = Point2.x(con.posTo) * tilesize, y2 = Point2.y(con.posTo) * tilesize,
                                    mx = (cx * clusterSize + clusterSize/2f) * tilesize, my = (cy * clusterSize + clusterSize/2f) * tilesize;
                                    //Lines.curve(x1, y1, mx, my, mx, my, x2, y2, 20);
                                    Lines.line(x1, y1, x2, y2);
                                }
                            }
                        }
                    }
                    Draw.reset();
                });
            });
        }
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
            cluster.innerEdges.clear();
        }

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

    static PathfindQueue frontier = new PathfindQueue();
    //node index -> total cost
    static IntFloatMap costs = new IntFloatMap();
    
    static IntSet usedEdges = new IntSet();

    void connectInnerEdges(int cx, int cy, int team, PathCost cost, Cluster cluster){
        int minX = cx * clusterSize, minY = cy * clusterSize, maxX = Math.min(minX + clusterSize - 1, wwidth - 1), maxY = Math.min(minY + clusterSize - 1, wheight - 1);
        
        usedEdges.clear();
        cluster.cons.clear();

        //TODO: how the hell to identify a vertex?
        //cluster (i16) | direction (i2) | index (i14)

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

                            //HOW
                            if(Point2.pack(x, y) == Point2.pack(otherX, otherY)){
                                if(true) continue;

                                Log.infoList("self ", direction, " ", i, " | ", otherDir, " ", j);
                                System.exit(1);
                            }

                            float connectionCost = astar(
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
                                cluster.cons.add(new Con(Point2.pack(x, y), Point2.pack(otherX, otherY), connectionCost));

                                Fx.debugLine.at(x* tilesize, y * tilesize, 0f, Color.purple,
                                new Vec2[]{new Vec2(x, y).scl(tilesize), new Vec2(otherX, otherY).scl(tilesize)});
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
    float astar(int team, PathCost cost, int minX, int minY, int maxX, int maxY, int startPos, int goalPos, int goalX1, int goalY1, int goalX2, int goalY2){
        frontier.clear();
        costs.clear();

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
                float currentCost = costs.get(current);

                if(add < 0) continue;

                float newCost = currentCost + add;

                //a cost of 0 means "not set"
                if(!costs.containsKey(next) || newCost < costs.get(next)){
                    costs.put(next, newCost);
                    float priority = newCost + heuristic(next, goalPos);
                    frontier.add(next, priority);
                }
            }
        }

        return -1f;
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
        IntSeq innerEdges = new IntSeq();
        Seq<Con> cons = new Seq<>();
    }

    //TODO for debugging only
    static class Con{
        int posFrom, posTo;
        float cost;

        public Con(int posFrom, int posTo, float cost){
            this.posFrom = posFrom;
            this.posTo = posTo;
            this.cost = cost;
        }
    }
}
