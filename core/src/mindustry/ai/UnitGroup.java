package mindustry.ai;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.Pathfinder.*;
import mindustry.async.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.gen.*;

public class UnitGroup{
    public Seq<Unit> units = new Seq<>();
    public int collisionLayer;
    public volatile float[] positions, originalPositions;
    public volatile boolean valid;
    
    public void calculateFormation(Vec2 dest, int collisionLayer){
        this.collisionLayer = collisionLayer;

        float cx = 0f, cy = 0f;
        for(Unit unit : units){
            cx += unit.x;
            cy += unit.y;
        }
        cx /= units.size;
        cy /= units.size;
        positions = new float[units.size * 2];


        //all positions are relative to the center
        for(int i = 0; i < units.size; i ++){
            Unit unit = units.get(i);
            positions[i * 2] = unit.x - cx;
            positions[i * 2 + 1] = unit.y - cy;
            unit.command().groupIndex = i;
        }

        //run on new thread to prevent stutter
        Vars.mainExecutor.submit(() -> {
            //unused space between circles that needs to be reached for compression to end
            float maxSpaceUsage = 0.7f;
            boolean compress = true;

            int compressionIterations = 0;
            int physicsIterations = 0;
            int totalIterations = 0;
            int maxPhysicsIterations = Math.min(1 + (int)(Math.pow(units.size, 0.65) / 10), 6);

            //yep, new allocations, because this is a new thread.
            IntQuadTree tree = new IntQuadTree(new Rect(0f, 0f, Vars.world.unitWidth(), Vars.world.unitHeight()),
                (index, hitbox) -> hitbox.setCentered(positions[index * 2], positions[index * 2 + 1], units.get(index).hitSize));
            IntSeq tmpseq = new IntSeq();
            Vec2 v1 = new Vec2();
            Vec2 v2 = new Vec2();

            //this algorithm basically squeezes all the circle colliders together, then proceeds to simulate physics to push them apart across several iterations.
            //it's rather slow, but shouldn't be too much of an issue when run in a different thread
            while(totalIterations++ < 40 && physicsIterations < maxPhysicsIterations){
                float spaceUsed = 0f;

                if(compress){
                    compressionIterations ++;

                    float maxDst = 1f, totalArea = 0f;
                    for(int a = 0; a < units.size; a ++){
                        v1.set(positions[a * 2], positions[a * 2 + 1]).lerp(v2.set(0f, 0f), 0.3f);
                        positions[a * 2] = v1.x;
                        positions[a * 2 + 1] = v1.y;

                        float rad = units.get(a).hitSize/2f;

                        maxDst = Math.max(maxDst, v1.dst(0f, 0f) + rad);
                        totalArea += Mathf.PI * rad * rad;
                    }

                    //total area of bounding circle
                    float boundingArea = Mathf.PI * maxDst * maxDst;
                    spaceUsed = totalArea / boundingArea;

                    //ex: 60% (0.6) of the total area is used, this will not be enough to satisfy a maxSpaceUsage of 70% (0.7)
                    compress = spaceUsed <= maxSpaceUsage && compressionIterations < 20;
                }

                //uncompress units
                if(!compress || spaceUsed > 0.5f){
                    physicsIterations++;

                    tree.clear();

                    for(int a = 0; a < units.size; a++){
                        tree.insert(a);
                    }

                    for(int a = 0; a < units.size; a++){
                        Unit unit = units.get(a);
                        float x = positions[a * 2], y = positions[a * 2 + 1], radius = unit.hitSize/2f;

                        tmpseq.clear();
                        tree.intersect(x - radius, y - radius, radius * 2f, radius * 2f, tmpseq);
                        for(int res = 0; res < tmpseq.size; res ++){
                            int b = tmpseq.items[res];

                            //simulate collision physics
                            if(a != b){
                                float ox = positions[b * 2], oy = positions[b * 2 + 1];
                                Unit other = units.get(b);

                                float rs = (radius + other.hitSize/2f) * 1.2f;
                                float dst = Mathf.dst(x, y, ox, oy);

                                if(dst < rs){
                                    v2.set(x - ox, y - oy).setLength(rs - dst);
                                    float mass1 = unit.hitSize, mass2 = other.hitSize;
                                    float ms = mass1 + mass2;
                                    float m1 = mass2 / ms, m2 = mass1 / ms;
                                    float scl = 1f;

                                    positions[a * 2] += v2.x * m1 * scl;
                                    positions[a * 2 + 1] += v2.y * m1 * scl;

                                    positions[b * 2] -= v2.x * m2 * scl;
                                    positions[b * 2 + 1] -= v2.y * m2 * scl;
                                }
                            }
                        }
                    }
                }
            }

            originalPositions = positions.clone();

            //raycast from the destination to the offset to make sure it's reachable
            for(int a = 0; a < units.size; a ++){
                updateRaycast(a, dest, v1);
            }

            valid = true;

            if(ControlPathfinder.showDebug){
                Core.app.post(() -> {
                    for(int i = 0; i < units.size; i ++){
                        float x = positions[i * 2], y = positions[i * 2 + 1];

                        Fx.placeBlock.at(x + dest.x, y + dest.y, 1f, Color.green);
                    }
                });
            }
        });
    }

    public void updateRaycast(int index, Vec2 dest){
        updateRaycast(index, dest, Tmp.v1);
    }

    private void updateRaycast(int index, Vec2 dest, Vec2 v1){
        if(collisionLayer != PhysicsProcess.layerFlying){

            //coordinates in world space
            float
                x = originalPositions[index * 2] + dest.x,
                y = originalPositions[index * 2 + 1] + dest.y;

            Unit unit = units.get(index);

            PathCost cost = unit.type.pathCost;
            int res = ControlPathfinder.raycastFastAvoid(unit.team.id, cost, World.toTile(dest.x), World.toTile(dest.y), World.toTile(x), World.toTile(y));

            //collision found, make the destination the point right before the collision
            if(res != 0){
                v1.set(Point2.x(res) * Vars.tilesize - dest.x, Point2.y(res) * Vars.tilesize - dest.y);
                v1.setLength(Math.max(v1.len() - Vars.tilesize - 4f, 0));
                positions[index * 2] = v1.x;
                positions[index * 2 + 1] = v1.y;
            }

            if(ControlPathfinder.showDebug){
                Core.app.post(() -> Fx.debugLine.at(unit.x, unit.y, 0f, Color.green, new Vec2[]{new Vec2(dest.x, dest.y), new Vec2(x, y)}));
            }
        }
    }
}
