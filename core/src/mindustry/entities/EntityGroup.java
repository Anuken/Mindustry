package mindustry.entities;

import arc.*;
import arc.func.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.gen.*;

import java.util.*;

import static mindustry.Vars.collisions;

/** Represents a group of a certain type of entity.*/
@SuppressWarnings("unchecked")
public class EntityGroup<T extends Entityc> implements Iterable<T>{
    private static int lastId = 0;

    private final Seq<T> array;
    private final Seq<T> intersectArray = new Seq<>();
    private final Rect viewport = new Rect();
    private final Rect intersectRect = new Rect();
    private IntMap<T> map;
    private QuadTree tree;
    private boolean clearing;

    private int index;

    public static int nextId(){
        return lastId++;
    }

    public EntityGroup(Class<T> type, boolean spatial, boolean mapping){
        array = new Seq<>(false, 32, type);

        if(spatial){
            tree = new QuadTree<>(new Rect(0, 0, 0, 0));
        }

        if(mapping){
            map = new IntMap<>();
        }
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
        each(Entityc::update);
    }

    public void copy(Seq<T> arr){
        arr.addAll(array);
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

        each(e -> {
            Drawc draw = (Drawc)e;
            if(viewport.overlaps(draw.x() - draw.clipSize()/2f, draw.y() - draw.clipSize()/2f, draw.clipSize(), draw.clipSize())){
                cons.get(e);
            }
        });
    }

    public boolean useTree(){
        return map != null;
    }

    public boolean mappingEnabled(){
        return map != null;
    }

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

    public void remove(T type){
        if(clearing) return;
        if(type == null) throw new RuntimeException("Cannot remove a null entity!");
        int idx = array.indexOf(type, true);
        if(idx != -1){
            array.remove(idx);
            if(map != null){
                map.remove(type.id());
            }

            //fix iteration index when removing
            if(index >= idx){
                index --;
            }
        }
    }

    public void clear(){
        clearing = true;

        array.each(Entityc::remove);
        array.clear();
        if(map != null)
            map.clear();

        clearing = false;
    }

    public T find(Boolf<T> pred){
        return array.find(pred);
    }

    @Override
    public Iterator<T> iterator(){
        return array.iterator();
    }
}
