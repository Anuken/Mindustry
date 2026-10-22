package arc.math.geom;

import arc.func.*;
import arc.struct.*;

public class IntQuadTree{
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