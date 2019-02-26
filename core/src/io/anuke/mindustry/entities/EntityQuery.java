package io.anuke.mindustry.entities;

import io.anuke.arc.collection.Array;
import io.anuke.arc.function.Consumer;
import io.anuke.arc.function.Predicate;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Rectangle;
import io.anuke.mindustry.entities.traits.Entity;
import io.anuke.mindustry.entities.traits.SolidTrait;

import static io.anuke.mindustry.entities.Entities.defaultGroup;

public class EntityQuery{
    private static final EntityCollisions collisions = new EntityCollisions();
    private static final Array<SolidTrait> array = new Array<>();
    private static final Rectangle r1 = new Rectangle();

    public static EntityCollisions collisions(){
        return collisions;
    }

    public static void init(float x, float y, float w, float h){

        for(EntityGroup group : Entities.getAllGroups()){
            if(group.useTree()){
                group.setTree(x, y, w, h);
            }
        }
    }

    public static void init(){
        init(0, 0, 0, 0);
    }

    public static void resizeTree(float x, float y, float w, float h){
        init(x, y, w, h);
    }

    public static void getNearby(EntityGroup<?> group, Rectangle rect, Consumer<SolidTrait> out){

        if(!group.useTree())
            throw new RuntimeException("This group does not support quadtrees! Enable quadtrees when creating it.");
        group.tree().getIntersect(out, rect);
    }

    public static Array<SolidTrait> getNearby(EntityGroup<?> group, Rectangle rect){

        array.clear();
        if(!group.useTree())
            throw new RuntimeException("This group does not support quadtrees! Enable quadtrees when creating it.");
        group.tree().getIntersect(array, rect);
        return array;
    }

    public static void getNearby(float x, float y, float size, Consumer<SolidTrait> out){
        getNearby(defaultGroup(), r1.setSize(size).setCenter(x, y), out);
    }

    public static void getNearby(EntityGroup<?> group, float x, float y, float size, Consumer<SolidTrait> out){
        getNearby(group, r1.setSize(size).setCenter(x, y), out);
    }

    public static Array<SolidTrait> getNearby(float x, float y, float size){
        return getNearby(defaultGroup(), r1.setSize(size).setCenter(x, y));
    }

    public static Array<SolidTrait> getNearby(EntityGroup<?> group, float x, float y, float size){
        return getNearby(group, r1.setSize(size).setCenter(x, y));
    }

    public static <T extends Entity> T getClosest(EntityGroup<T> group, float x, float y, float range, Predicate<T> pred){

        T closest = null;
        float cdist = 0f;
        Array<SolidTrait> entities = getNearby(group, x, y, range * 2f);
        for(int i = 0; i < entities.size; i++){
            T e = (T) entities.get(i);
            if(!pred.test(e))
                continue;

            float dist = Mathf.dst(e.getX(), e.getY(), x, y);
            if(dist < range)
                if(closest == null || dist < cdist){
                    closest = e;
                    cdist = dist;
                }
        }

        return closest;
    }

    public static void collideGroups(EntityGroup<?> groupa, EntityGroup<?> groupb){
        collisions().collideGroups(groupa, groupb);
    }
}
