package mindustry.entities;

import arc.*;
import arc.func.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Time.*;
import arc.util.pooling.*;
import mindustry.gen.*;

import java.util.*;

import static mindustry.Vars.*;

/** Represents a group of a certain type of entity.*/
@SuppressWarnings("unchecked")
public class EntityGroup<T extends Entityc> implements Iterable<T>{
    private static int lastId = 0;

    private final Seq<T> array;
    private final Seq<T> intersectArray = new Seq<>();
    private final Rect viewport = new Rect();
    private final Rect intersectRect = new Rect();
    private final EntityIndexer indexer;
    private IntMap<T> map;
    private QuadTree tree;
    private boolean clearing;

    private int index;

    private double fixedCounter, timeCounter;
    private long lastTimeAccess = -1;
    private long totalUpdates = 0, updateId;
    private float lastRenderInterpolation = 1f;
    private Seq<DelayRun> timeRuns = new Seq<>();

    public static int nextId(){
        if(lastId >= Integer.MAX_VALUE - 2) lastId = 0;
        return lastId++;
    }

    /** Makes sure the next ID counter is higher than this number, so future entities cannot possibly use this ID. */
    public static void checkNextId(int id){
        lastId = Math.max(lastId, id + 1);
    }

    public EntityGroup(Class<T> type, boolean spatial, boolean mapping){
        this(type, spatial, mapping, null);
    }

    public EntityGroup(Class<T> type, boolean spatial, boolean mapping, EntityIndexer indexer){
        array = new Seq<>(false, 32, type);

        if(spatial){
            tree = new QuadTree<>(new Rect(0, 0, 0, 0));
        }

        if(mapping){
            map = new IntMap<>();
        }

        this.indexer = indexer;
    }

    /** @return entities with colliding IDs, or an empty array. */
    public Seq<T> checkIDCollisions(){
        Seq<T> out = new Seq<>();
        IntSet ints = new IntSet();
        each(u -> {
            if(!ints.add(u.id())){
                out.add(u);
            }
        });
        return out;
    }

    public void sort(Comparator<? super T> comp){
        array.sort(comp);
    }

    public void collide(){
        collisions.collide((EntityGroup<? extends Hitboxc>)this);
    }

    public void updatePhysics(){
        collisions.updatePhysics((EntityGroup<? extends Hitboxc>)this);
    }

    public void update(){
        for(index = 0; index < array.size; index++){
            array.items[index].update();
        }
    }

    /** Calls {@link #fixedUpdate(int, int)} with a minimum FPS of 10. */
    public void fixedUpdate(int targetFps){
        fixedUpdate(targetFps, Math.max(2, targetFps / 10));
    }

    /**
     * Updates this entity group at a fixed rate and delta time, regardless of framerate.
     * If updates per frame exceed {@param maxUpdatesPerFrame}, they will be skipped - visually, the game will look like it is slowing down.
     * For example, a value of 60 targetUps and 10 maxUpdatesPerFrame will mean that the game only starts slowing down below (60 / 10) = 6 FPS.
     * */
    public void fixedUpdate(int targetUps, int maxUpdatesPerFrame){
        //if fixedUpdate isn't called, e.g. when the game is paused or map is reloaded, the time counter needs to 'sync' with the actual proper time
        if(lastTimeAccess != Core.graphics.getFrameId()){
            totalUpdates = 0;
            updateId = state.updateId;
            timeCounter = Time.getInternalTime();
        }

        long prevUpdateId = state.updateId;
        double targetDelta = 1.0 / targetUps;
        float timeDelta = (float)targetDelta * 60f;
        float prevDelta = Time.delta;
        double prevTime = Time.getInternalTime();
        var oldRuns = Time.getRuns();

        //since some logic (incorrectly!) relies on Time.time, it has to be passed like this across several variables.
        Time.delta = timeDelta;
        Time.setInternalTime(timeCounter);
        Time.setRuns(timeRuns);

        float delta = Core.graphics.getDeltaTime();

        //the first two updates are usually bogus
        if(totalUpdates++ < 2) delta = Math.min(delta, 1f / 60f);

        fixedCounter += delta;
        fixedCounter = Math.min(fixedCounter, targetDelta * maxUpdatesPerFrame);

        while(fixedCounter >= targetDelta){
            //this executes any pending tasks (manually reassigned), and increments internal time, which is local to this group
            Time.update();
            update();
            fixedCounter -= targetDelta;
            state.updateId = updateId ++;
        }

        timeCounter = Time.getInternalTime();

        Time.delta = prevDelta;
        Time.setInternalTime(prevTime);
        Time.setRuns(oldRuns);
        state.updateId = prevUpdateId;

        lastTimeAccess = Core.graphics.getFrameId();
        lastRenderInterpolation = (float)(fixedCounter / targetDelta);
    }

    public long getFixedUpdateId(){
        return updateId;
    }

    public float getRenderInterpolation(){
        return lastRenderInterpolation;
    }

    public Seq<T> copy(){
        return copy(new Seq<>());
    }

    public Seq<T> copy(Seq<T> arr){
        arr.addAll(array);
        return arr;
    }

    public void each(Cons<T> cons){
        for(index = 0; index < array.size; index++){
            cons.get(array.items[index]);
        }
    }

    public void each(Boolf<T> filter, Cons<T> cons){
        for(index = 0; index < array.size; index++){
            if(filter.get(array.items[index])) cons.get(array.items[index]);
        }
    }

    public void draw(Cons<T> cons){
        Core.camera.bounds(viewport);

        for(index = 0; index < array.size; index++){
            Drawc draw = (Drawc)array.items[index];
            float clip = draw.clipSize();
            if(viewport.overlaps(draw.x() - clip/2f, draw.y() - clip/2f, clip, clip)){
                cons.get((T)draw);
            }
        }
    }

    public boolean useTree(){
        return tree != null;
    }

    public boolean mappingEnabled(){
        return map != null;
    }

    @Nullable
    public T getByID(int id){
        if(map == null) throw new RuntimeException("Mapping is not enabled for group " + id + "!");
        return map.get(id);
    }

    public void removeByID(int id){
        if(map == null) throw new RuntimeException("Mapping is not enabled for group " + id + "!");
        T t = map.get(id);
        if(t != null){ //remove if present in map already
            t.remove();
        }
    }

    public void intersect(float x, float y, float width, float height, Cons<? super T> out){
        //don't waste time for empty groups
        if(isEmpty()) return;
        tree.intersect(x, y, width, height, out);
    }

    public boolean intersect(float x, float y, float width, float height, Boolf<? super T> out){
        //don't waste time for empty groups
        if(isEmpty()) return false;
        return tree.intersect(x, y, width, height, out);
    }

    public Seq<T> intersect(float x, float y, float width, float height){
        intersectArray.clear();
        //don't waste time for empty groups
        if(isEmpty()) return intersectArray;
        tree.intersect(intersectRect.set(x, y, width, height), intersectArray);
        return intersectArray;
    }

    public QuadTree tree(){
        if(tree == null) throw new RuntimeException("This group does not support quadtrees! Enable quadtrees when creating it.");
        return tree;
    }

    /** Resizes the internal quadtree, if it is enabled.*/
    public void resize(float x, float y, float w, float h){
        if(tree != null){
            tree = new QuadTree<>(new Rect(x, y, w, h));
        }
    }

    public boolean isEmpty(){
        return array.size == 0;
    }

    public T index(int i){
        return array.get(i);
    }

    public int size(){
        return array.size;
    }

    public boolean contains(Boolf<T> pred){
        return array.contains(pred);
    }

    public int count(Boolf<T> pred){
        return array.count(pred);
    }

    public void add(T type){
        if(type == null) throw new RuntimeException("Cannot add a null entity!");
        array.add(type);

        if(mappingEnabled()){
            map.put(type.id(), type);
        }
    }

    public int addIndex(T type){
        int index = array.size;
        add(type);
        return index;
    }

    public void remove(T type){
        if(clearing) return;
        if(type == null) throw new RuntimeException("Cannot remove a null entity!");
        int idx = array.indexOf(type, true);
        if(idx != -1){
            array.remove(idx);

            //fix incorrect HEAD index since it was swapped
            if(array.size > 0 && idx != array.size){
                var swapped = array.items[idx];
                if(indexer != null) indexer.change(swapped, idx);
            }

            if(map != null){
                map.remove(type.id());
            }

            //fix iteration index when removing
            if(index >= idx){
                index --;
            }
        }
    }

    public void removeIndex(T type, int position){
        if(clearing) return;
        if(type == null) throw new RuntimeException("Cannot remove a null entity!");
        if(position != -1 && position < array.size){

            //rarely the entity index is wrong; fallback to slow implementation
            if(array.items[position] != type){
                remove(type);
                return;
            }

            //swap head with current
            if(array.size > 1){
                var head = array.items[array.size - 1];
                if(indexer != null) indexer.change(head, position);
                array.items[position] = head;
            }

            array.size --;
            array.items[array.size] = null;

            if(map != null){
                map.remove(type.id());
            }

            //fix iteration index when removing
            if(index >= position){
                index --;
            }
        }
    }

    public void clear(){
        clearing = true;

        array.each(Entityc::remove);
        array.clear();
        if(map != null) map.clear();
        Pools.freeAll(timeRuns, true);
        timeRuns.clear();

        clearing = false;
        totalUpdates = 0;

        lastRenderInterpolation = 0f;
        lastTimeAccess = -1;
        updateId = 0;
    }

    @Nullable
    public T find(Boolf<T> pred){
        return array.find(pred);
    }

    @Nullable
    public T first(){
        return array.first();
    }

    @Override
    public Iterator<T> iterator(){
        return array.iterator();
    }
}
