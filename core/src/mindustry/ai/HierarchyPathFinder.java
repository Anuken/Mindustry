package mindustry.ai;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.graphics.*;

import static mindustry.Vars.*;
import static mindustry.ai.Pathfinder.*;

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
                                Draw.color(Color.blue);

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
                portals = cluster.portals[direction] = new IntSeq();
            }else{
                //share portals with the other cluster
                portals = cluster.portals[direction] = other.portals[(direction + 2) % 4];
            }

            //Point2 adder = Geometry.d4[(direction + 1) % 4];

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

        Cluster(){

        }


    }
}
