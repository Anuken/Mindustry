package mindustry.entities;

import arc.struct.*;
import arc.util.pooling.*;
import arc.util.pooling.Pool.*;

/**
 * Quad tree for optimized queries in 2d space
 * @author Piotr-J
 */
public class QuadTree2 implements Poolable{
    /**
     * Max count of containers in a tree before it is split
     * <p>
     * Should be tweaked for best performance
     */
    public static int MAX_IN_BUCKET = 16;
    /**
     * Max count of splits, tree start at depth = 0
     * <p>
     * Should be tweaked for best performance
     */
    public static int MAX_DEPTH = 8;

    private static Pool<QuadTree2> qtPool = Pools.get(QuadTree2.class, QuadTree2::new);
    private static Pool<Container> cPool = Pools.get(Container.class, Container::new);
    private static Array<Container> idToContainer = new Array<>();

    public final static int OUTSIDE = -1;
    public final static int SW = 0;
    public final static int SE = 1;
    public final static int NW = 2;
    public final static int NE = 3;
    protected int depth;
    protected Array<Container> containers;
    protected Container bounds;
    protected QuadTree2[] nodes;
    protected QuadTree2 parent;

    /**
     * Public constructor for {@link Pool} use only
     */
    public QuadTree2(){
        this(0, 0, 0, 0);
    }

    /**
     * Public constructor for initial {@link QuadTree2}
     * <p>
     * Specify max tree bounds
     */
    public QuadTree2(float x, float y, float width, float height){
        bounds = new Container();
        containers = new Array<>(MAX_IN_BUCKET);
        nodes = new QuadTree2[4];
        init(0, x, y, width, height, null);
    }

    protected QuadTree2 init(int depth, float x, float y, float width, float height, QuadTree2 parent){
        this.depth = depth;
        bounds.set(x, y, width, height);
        this.parent = parent;
        return this;
    }

    private int indexOf(float x, float y, float width, float height){
        float midX = bounds.x + bounds.width / 2;
        float midY = bounds.y + bounds.height / 2;
        boolean top = y > midY;
        boolean bottom = y < midY && y + height < midY;
        if(x < midX && x + width < midX){
            if(top){
                return NW;
            }else if(bottom){
                return SW;
            }
        }else if(x > midX){
            if(top){
                return NE;
            }else if(bottom){
                return SE;
            }
        }
        return OUTSIDE;
    }

    /**
     * Inserts given entity id to tree with given bounds
     */
    public void insert(int eid, float x, float y, float width, float height){
        insert(cPool.obtain().set(eid, x, y, width, height));
    }

    protected void insert(Container c){
        if(nodes[0] != null){
            int index = indexOf(c.x, c.y, c.width, c.height);
            if(index != OUTSIDE){
                nodes[index].insert(c);
                return;
            }
        }
        c.parent = this;
        idToContainer.set(c.eid, c);
        containers.add(c);

        if(containers.size > MAX_IN_BUCKET && depth < MAX_DEPTH){
            if(nodes[0] == null){
                float halfWidth = bounds.width / 2;
                float halfHeight = bounds.height / 2;
                nodes[SW] = qtPool.obtain().init(depth + 1, bounds.x, bounds.y, halfWidth, halfHeight, this);
                nodes[SE] = qtPool.obtain().init(depth + 1, bounds.x + halfWidth, bounds.y, halfWidth, halfHeight, this);
                nodes[NW] = qtPool.obtain().init(depth + 1, bounds.x, bounds.y + halfHeight, halfWidth, halfHeight, this);
                nodes[NE] = qtPool.obtain().init(depth + 1, bounds.x + halfWidth, bounds.y + halfHeight, halfWidth, halfHeight, this);
            }

            for(int i = containers.size - 1; i >= 0; i--){
                Container next = containers.get(i);
                int index = indexOf(next.x, next.y, next.width, next.height);
                if(index != OUTSIDE){
                    nodes[index].insert(next);
                    containers.remove(i);
                }
            }
        }
    }

    /**
     * Returns entity ids of entities that are inside {@link QuadTree2}s that contain given point
     * <p>
     * Returned entities must be filtered further as these results are not exact
     */
    public IntArray get(IntArray fill, float x, float y){
        if(bounds.contains(x, y)){
            if(nodes[0] != null){
                int index = indexOf(x, y, 0, 0);
                if(index != OUTSIDE){
                    nodes[index].get(fill, x, y, 0, 0);
                }
            }
            for(int i = 0; i < containers.size; i++){
                fill.add(containers.get(i).eid);
            }
        }
        return fill;
    }

    /**
     * Returns entity ids of entities that bounds contain given point
     */
    public IntArray getExact(IntArray fill, float x, float y){
        if(bounds.contains(x, y)){
            if(nodes[0] != null){
                int index = indexOf(x, y, 0, 0);
                if(index != OUTSIDE){
                    nodes[index].getExact(fill, x, y, 0, 0);
                }
            }
            for(int i = 0; i < containers.size; i++){
                Container c = containers.get(i);
                if(c.contains(x, y)){
                    fill.add(c.eid);
                }
            }
        }
        return fill;
    }

    /**
     * Returns entity ids of entities that are inside {@link QuadTree2}s that overlap given bounds
     * <p>
     * Returned entities must be filtered further as these results are not exact
     */
    public IntArray get(IntArray fill, float x, float y, float width, float height){
        if(bounds.overlaps(x, y, width, height)){
            if(nodes[0] != null){
                int index = indexOf(x, y, width, height);
                if(index != OUTSIDE){
                    nodes[index].get(fill, x, y, width, height);
                }else{
                    // if test bounds don't fully fit inside a node, we need to check them all
                    for(QuadTree2 node : nodes){
                        node.get(fill, x, y, width, height);
                    }
                }
            }
            for(int i = 0; i < containers.size; i++){
                Container c = containers.get(i);
                fill.add(c.eid);
            }
        }
        return fill;
    }

    /**
     * Returns entity ids of entities that overlap given bounds
     */
    public IntArray getExact(IntArray fill, float x, float y, float width, float height){
        if(bounds.overlaps(x, y, width, height)){
            if(nodes[0] != null){
                int index = indexOf(x, y, width, height);
                if(index != OUTSIDE){
                    nodes[index].getExact(fill, x, y, width, height);
                }else{
                    // if test bounds don't fully fit inside a node, we need to check them all
                    for(QuadTree2 node : nodes){
                        node.getExact(fill, x, y, width, height);
                    }
                }
            }
            for(int i = 0; i < containers.size; i++){
                Container c = containers.get(i);
                if(c.overlaps(x, y, width, height)){
                    fill.add(c.eid);
                }
            }
        }
        return fill;
    }

    /**
     * Update position for this id with new one
     */
    public void update(int id, float x, float y, float width, float height){
        Container c = idToContainer.get(id);
        c.set(id, x, y, width, height);

        QuadTree2 qTree = c.parent;
        qTree.containers.remove(c);
        while(qTree.parent != null && !qTree.bounds.contains(c)){
            qTree = qTree.parent;
        }
        qTree.insert(c);
    }

    /**
     * Remove this id from the tree
     */
    public void remove(int id){
        Container c = idToContainer.get(id);
        if(c == null)
            return;
        if(c.parent != null){
            c.parent.containers.remove(c);
        }
        cPool.free(c);
    }

    /**
     * Reset the QuadTree by removing all nodes and stored ids
     */
    @Override
    public void reset(){
        for(int i = containers.size - 1; i >= 0; i--){
            cPool.free(containers.remove(i));
        }
        for(int i = 0; i < nodes.length; i++){
            if(nodes[i] != null){
                qtPool.free(nodes[i]);
                nodes[i] = null;
            }
        }
    }

    /**
     * Dispose of the QuadTree by removing all nodes and stored ids
     */
    public void dispose(){
        reset();
    }

    /**
     * @return {@link QuadTree2[]} with nodes of these tree, nodes may be null
     */
    public QuadTree2[] getNodes(){
        return nodes;
    }

    /**
     * @return {@link Container} that represents bounds of this tree
     */
    public Container getBounds(){
        return bounds;
    }

    @Override
    public String toString(){
        return "QuadTree{" +
        "depth=" + depth + "}";
    }

    /** Simple container for entity ids and their bounds*/
    static class Container implements Poolable{
        private int eid;
        private float x;
        private float y;
        private float width;
        private float height;
        private QuadTree2 parent;

        public Container(){
        }

        public Container set(int eid, float x, float y, float width, float height){
            this.eid = eid;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public Container set(float x, float y, float width, float height){
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public boolean contains(float x, float y){
            return this.x <= x && this.x + this.width >= x && this.y <= y && this.y + this.height >= y;
        }

        public boolean overlaps(float x, float y, float width, float height){
            return this.x < x + width && this.x + this.width > x && this.y < y + height && this.y + this.height > y;
        }

        public boolean contains(float ox, float oy, float owidth, float oheight){
            float xmax = ox + owidth;
            float ymax = oy + oheight;

            return ((ox > x && ox < x + width) && (xmax > x && xmax < x + width)) && ((oy > y && oy < y + height) && (ymax > y && ymax < y + height));
        }

        public boolean contains(Container c){
            return contains(c.x, c.y, c.width, c.height);
        }

        @Override
        public void reset(){
            eid = -1;
            x = 0;
            y = 0;
            width = 0;
            height = 0;
            parent = null;
        }
    }
}