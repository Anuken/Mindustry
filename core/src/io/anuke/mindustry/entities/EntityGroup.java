package io.anuke.mindustry.entities;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.func.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.math.geom.*;
import io.anuke.mindustry.entities.traits.*;

import static io.anuke.mindustry.Vars.collisions;

/** Represents a group of a certain type of entity.*/
@SuppressWarnings("unchecked")
public class EntityGroup<T extends Entity>{
    private final boolean useTree;
    private final int id;
    private final Class<T> type;
    private final Array<T> entityArray = new Array<>(false, 32);
    private final Array<T> entitiesToRemove = new Array<>(false, 32);
    private final Array<T> entitiesToAdd = new Array<>(false, 32);
    private final Array<T> intersectArray = new Array<>();
    private final Rectangle intersectRect = new Rectangle();
    private IntMap<T> map;
    private QuadTree tree;
    private Cons<T> removeListener;
    private Cons<T> addListener;

    private final Rectangle viewport = new Rectangle();
    private int count = 0;

    public EntityGroup(int id, Class<T> type, boolean useTree){
        this.useTree = useTree;
        this.id = id;
        this.type = type;

        if(useTree){
            tree = new QuadTree<>(new Rectangle(0, 0, 0, 0));
        }
    }

    public void update(){
        updateEvents();

        if(useTree()){
            collisions.updatePhysics(this);
        }

        for(Entity e : all()){
            e.update();
        }
    }

    public int countInBounds(){
        count = 0;
        draw(e -> true, e -> count++);
        return count;
    }

    public void draw(){
        draw(e -> true);
    }

    public void draw(Boolf<T> toDraw){
        draw(toDraw, t -> ((DrawTrait)t).draw());
    }

    public void draw(Boolf<T> toDraw, Cons<T> cons){
        Camera cam = Core.camera;
        viewport.set(cam.position.x - cam.width / 2, cam.position.y - cam.height / 2, cam.width, cam.height);

        for(Entity e : all()){
            if(!(e instanceof DrawTrait) || !toDraw.get((T)e) || !e.isAdded()) continue;
            DrawTrait draw = (DrawTrait)e;

            if(viewport.overlaps(draw.getX() - draw.drawSize()/2f, draw.getY() - draw.drawSize()/2f, draw.drawSize(), draw.drawSize())){
                cons.get((T)e);
            }
        }
    }

    public boolean useTree(){
        return useTree;
    }

    public void setRemoveListener(Cons<T> removeListener){
        this.removeListener = removeListener;
    }

    public void setAddListener(Cons<T> addListener){
        this.addListener = addListener;
    }

    public EntityGroup<T> enableMapping(){
        map = new IntMap<>();
        return this;
    }

    public boolean mappingEnabled(){
        return map != null;
    }

    public Class<T> getType(){
        return type;
    }

    public int getID(){
        return id;
    }

    public void updateEvents(){

        for(T e : entitiesToAdd){
            if(e == null)
                continue;
            entityArray.add(e);
            e.added();

            if(map != null){
                map.put(e.getID(), e);
            }
        }

        entitiesToAdd.clear();

        for(T e : entitiesToRemove){
            entityArray.removeValue(e, true);
            if(map != null){
                map.remove(e.getID());
            }
            e.removed();
        }

        entitiesToRemove.clear();
    }

    public T getByID(int id){
        if(map == null) throw new RuntimeException("Mapping is not enabled for group " + id + "!");
        return map.get(id);
    }

    public void removeByID(int id){
        if(map == null) throw new RuntimeException("Mapping is not enabled for group " + id + "!");
        T t = map.get(id);
        if(t != null){ //remove if present in map already
            remove(t);
        }else{ //maybe it's being queued?
            for(T check : entitiesToAdd){
                if(check.getID() == id){ //if it is indeed queued, remove it
                    entitiesToAdd.removeValue(check, true);
                    if(removeListener != null){
                        removeListener.get(check);
                    }
                    break;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void intersect(float x, float y, float width, float height, Cons<? super T> out){
        //don't waste time for empty groups
        if(isEmpty()) return;
        tree().getIntersect(out, x, y, width, height);
    }

    @SuppressWarnings("unchecked")
    public Array<T> intersect(float x, float y, float width, float height){
        intersectArray.clear();
        //don't waste time for empty groups
        if(isEmpty()) return intersectArray;
        tree().getIntersect(intersectArray, intersectRect.set(x, y, width, height));
        return intersectArray;
    }

    public QuadTree tree(){
        if(!useTree) throw new RuntimeException("This group does not support quadtrees! Enable quadtrees when creating it.");
        return tree;
    }

    /** Resizes the internal quadtree, if it is enabled.*/
    public void resize(float x, float y, float w, float h){
        if(useTree){
            tree = new QuadTree<>(new Rectangle(x, y, w, h));
        }
    }

    public boolean isEmpty(){
        return entityArray.size == 0;
    }

    public int size(){
        return entityArray.size;
    }

    public int count(Boolf<T> pred){
        int count = 0;
        for(int i = 0; i < entityArray.size; i++){
            if(pred.get(entityArray.get(i))) count++;
        }
        return count;
    }

    public void add(T type){
        if(type == null) throw new RuntimeException("Cannot add a null entity!");
        if(type.getGroup() != null) return;
        type.setGroup(this);
        entitiesToAdd.add(type);

        if(mappingEnabled()){
            map.put(type.getID(), type);
        }

        if(addListener != null){
            addListener.get(type);
        }
    }

    public void remove(T type){
        if(type == null) throw new RuntimeException("Cannot remove a null entity!");
        type.setGroup(null);
        entitiesToRemove.add(type);

        if(removeListener != null){
            removeListener.get(type);
        }
    }

    public void clear(){
        for(T entity : entityArray){
            entity.removed();
            entity.setGroup(null);
        }

        for(T entity : entitiesToAdd)
            entity.setGroup(null);

        for(T entity : entitiesToRemove)
            entity.setGroup(null);

        entitiesToAdd.clear();
        entitiesToRemove.clear();
        entityArray.clear();
        if(map != null)
            map.clear();
    }

    public T find(Boolf<T> pred){

        for(int i = 0; i < entityArray.size; i++){
            if(pred.get(entityArray.get(i))) return entityArray.get(i);
        }

        return null;
    }

    /** Returns the logic-only array for iteration. */
    public Array<T> all(){
        return entityArray;
    }
}
