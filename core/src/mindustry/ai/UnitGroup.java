package mindustry.ai;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.*;
import mindustry.ai.Pathfinder.*;
import mindustry.async.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.gen.*;

public class UnitGroup{
    public Seq<Unit> units = new Seq<>();
    public float[] positions;
    public volatile boolean valid;
    
    public void calculateFormation(Vec2 dest, int collisionLayer){

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

            //raycast from the destination to the offset to make sure it's reachable
            if(collisionLayer != PhysicsProcess.layerFlying){
                for(int a = 0; a < units.size; a ++){
                    //coordinates in world space
                    float
                    x = positions[a * 2] + dest.x,
                    y = positions[a * 2 + 1] + dest.y;

                    Unit unit = units.get(a);

                    PathCost cost = unit.type.pathCost;
                    int res = ControlPathfinder.raycastFast(unit.team.id, cost, World.toTile(dest.x), World.toTile(dest.y), World.toTile(x), World.toTile(y));

                    //collision found, make th destination the point right before the collision
                    if(res != 0){
                        v1.set(Point2.x(res) * Vars.tilesize - dest.x, Point2.y(res) * Vars.tilesize - dest.y);
                        v1.setLength(Math.max(v1.len() - Vars.tilesize - 4f, 0));
                        positions[a * 2] = v1.x;
                        positions[a * 2 + 1] = v1.y;
                    }

                    if(ControlPathfinder.showDebug){
                        Core.app.post(() -> Fx.debugLine.at(unit.x, unit.y, 0f, Color.green, new Vec2[]{new Vec2(dest.x, dest.y), new Vec2(x, y)}));
                    }
                }
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

    public static class IntQuadTree{
        protected final Rect tmp = new Rect();
        protected static final int maxObjectsPerNode = 5;

        public IntQuadTreeProvider prov;
        public Rect bounds;
        public IntSeq objects = new IntSeq(false, 10);
        public IntQuadTree botLeft, botRight, topLeft, topRight;
        public boolean leaf = true;
        public int totalObjects;

        public IntQuadTree(Rect bounds, IntQuadTreeProvider prov){
            this.bounds = bounds;
            this.prov = prov;
        }

        protected void split(){
            if(!leaf) return;

            float subW = bounds.width / 2;
            float subH = bounds.height / 2;

            if(botLeft == null){
                botLeft = newChild(new Rect(bounds.x, bounds.y, subW, subH));
                botRight = newChild(new Rect(bounds.x + subW, bounds.y, subW, subH));
                topLeft = newChild(new Rect(bounds.x, bounds.y + subH, subW, subH));
                topRight = newChild(new Rect(bounds.x + subW, bounds.y + subH, subW, subH));
            }
            leaf = false;

            // Transfer objects to children if they fit entirely in one
            for(int i = 0; i < objects.size; i ++){
                int obj = objects.items[i];
                hitbox(obj);
                IntQuadTree child = getFittingChild(tmp);
                if(child != null){
                    child.insert(obj);
                    objects.removeIndex(i);
                    i --;
                }
            }
        }

        protected void unsplit(){
            if(leaf) return;
            objects.addAll(botLeft.objects);
            objects.addAll(botRight.objects);
            objects.addAll(topLeft.objects);
            objects.addAll(topRight.objects);
            botLeft.clear();
            botRight.clear();
            topLeft.clear();
            topRight.clear();
            leaf = true;
        }

        /**
         * Inserts an object into this node or its child nodes. This will split a leaf node if it exceeds the object limit.
         */
        public void insert(int obj){
            hitbox(obj);
            if(!bounds.overlaps(tmp)){
                // New object not in quad tree, ignoring
                // throw an exception?
                return;
            }

            totalObjects ++;

            if(leaf && objects.size + 1 > maxObjectsPerNode) split();

            if(leaf){
                // Leaf, so no need to add to children, just add to root
                objects.add(obj);
            }else{
                hitbox(obj);
                // Add to relevant child, or root if can't fit completely in a child
                IntQuadTree child = getFittingChild(tmp);
                if(child != null){
                    child.insert(obj);
                }else{
                    objects.add(obj);
                }
            }
        }

        /**
         * Removes an object from this node or its child nodes.
         */
        public boolean remove(int obj){
            boolean result;
            if(leaf){
                // Leaf, no children, remove from root
                result = objects.removeValue(obj);
            }else{
                // Remove from relevant child
                hitbox(obj);
                IntQuadTree child = getFittingChild(tmp);

                if(child != null){
                    result = child.remove(obj);
                }else{
                    // Or root if object doesn't fit in a child
                    result = objects.removeValue(obj);
                }

                if(totalObjects <= maxObjectsPerNode) unsplit();
            }
            if(result){
                totalObjects --;
            }
            return result;
        }

        /** Removes all objects. */
        public void clear(){
            objects.clear();
            totalObjects = 0;
            if(!leaf){
                topLeft.clear();
                topRight.clear();
                botLeft.clear();
                botRight.clear();
            }
            leaf = true;
        }

        protected IntQuadTree getFittingChild(Rect boundingBox){
            float verticalMidpoint = bounds.x + (bounds.width / 2);
            float horizontalMidpoint = bounds.y + (bounds.height / 2);

            // Object can completely fit within the top quadrants
            boolean topQuadrant = boundingBox.y > horizontalMidpoint;
            // Object can completely fit within the bottom quadrants
            boolean bottomQuadrant = boundingBox.y < horizontalMidpoint && (boundingBox.y + boundingBox.height) < horizontalMidpoint;

            // Object can completely fit within the left quadrants
            if(boundingBox.x < verticalMidpoint && boundingBox.x + boundingBox.width < verticalMidpoint){
                if(topQuadrant){
                    return topLeft;
                }else if(bottomQuadrant){
                    return botLeft;
                }
            }else if(boundingBox.x > verticalMidpoint){ // Object can completely fit within the right quadrants
                if(topQuadrant){
                    return topRight;
                }else if(bottomQuadrant){
                    return botRight;
                }
            }

            // Else, object needs to be in parent cause it can't fit completely in a quadrant
            return null;
        }

        /**
         * Processes objects that may intersect the given rectangle.
         * <p>
         * This will never result in false positives.
         */
        public void intersect(float x, float y, float width, float height, Intc out){
            if(!leaf){
                if(topLeft.bounds.overlaps(x, y, width, height)) topLeft.intersect(x, y, width, height, out);
                if(topRight.bounds.overlaps(x, y, width, height)) topRight.intersect(x, y, width, height, out);
                if(botLeft.bounds.overlaps(x, y, width, height)) botLeft.intersect(x, y, width, height, out);
                if(botRight.bounds.overlaps(x, y, width, height)) botRight.intersect(x, y, width, height, out);
            }

            IntSeq objects = this.objects;

            for(int i = 0; i < objects.size; i++){
                int item = objects.items[i];
                hitbox(item);
                if(tmp.overlaps(x, y, width, height)){
                    out.get(item);
                }
            }
        }

        /**
         * @return whether an object overlaps this rectangle.
         * This will never result in false positives.
         */
        public boolean any(float x, float y, float width, float height){
            if(!leaf){
                if(topLeft.bounds.overlaps(x, y, width, height) && topLeft.any(x, y, width, height)) return true;
                if(topRight.bounds.overlaps(x, y, width, height) && topRight.any(x, y, width, height)) return true;
                if(botLeft.bounds.overlaps(x, y, width, height) && botLeft.any(x, y, width, height)) return true;
                if(botRight.bounds.overlaps(x, y, width, height) && botRight.any(x, y, width, height))return true;
            }

            IntSeq objects = this.objects;

            for(int i = 0; i < objects.size; i++){
                int item = objects.items[i];
                hitbox(item);
                if(tmp.overlaps(x, y, width, height)){
                    return true;
                }
            }
            return false;
        }

        /**
         * Processes objects that may intersect the given rectangle.
         * <p>
         * This will never result in false positives.
         */
        public void intersect(Rect rect, Intc out){
            intersect(rect.x, rect.y, rect.width, rect.height, out);
        }

        /**
         * Fills the out parameter with any objects that may intersect the given rectangle.
         * <p>
         * This will result in false positives, but never a false negative.
         */
        public void intersect(Rect toCheck, IntSeq out){
            intersect(toCheck.x, toCheck.y, toCheck.width, toCheck.height, out);
        }

        /**
         * Fills the out parameter with any objects that may intersect the given rectangle.
         */
        public void intersect(float x, float y, float width, float height, IntSeq out){
            if(!leaf){
                if(topLeft.bounds.overlaps(x, y, width, height)) topLeft.intersect(x, y, width, height, out);
                if(topRight.bounds.overlaps(x, y, width, height)) topRight.intersect(x, y, width, height, out);
                if(botLeft.bounds.overlaps(x, y, width, height)) botLeft.intersect(x, y, width, height, out);
                if(botRight.bounds.overlaps(x, y, width, height)) botRight.intersect(x, y, width, height, out);
            }

            IntSeq objects = this.objects;

            for(int i = 0; i < objects.size; i++){
                int item = objects.items[i];
                hitbox(item);
                if(tmp.overlaps(x, y, width, height)){
                    out.add(item);
                }
            }
        }

        /** Adds all quadtree objects to the specified Seq. */
        public void getObjects(IntSeq out){
            out.addAll(objects);

            if(!leaf){
                topLeft.getObjects(out);
                topRight.getObjects(out);
                botLeft.getObjects(out);
                botRight.getObjects(out);
            }
        }

        protected IntQuadTree newChild(Rect rect){
            return new IntQuadTree(rect, prov);
        }

        protected void hitbox(int t){
            prov.hitbox(t, tmp);
        }

        /**Represents an object in a QuadTree.*/
        public interface IntQuadTreeProvider{
            /**Fills the out parameter with this element's rough bounding box. This should never be smaller than the actual object, but may be larger.*/
            void hitbox(int object, Rect out);
        }
    }

}
