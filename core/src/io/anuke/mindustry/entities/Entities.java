package io.anuke.mindustry.entities;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.IntMap;
import io.anuke.arc.function.Consumer;
import io.anuke.arc.function.Predicate;
import io.anuke.arc.graphics.Camera;
import io.anuke.arc.math.geom.Rectangle;
import io.anuke.mindustry.entities.traits.DrawTrait;
import io.anuke.mindustry.entities.traits.Entity;

import static io.anuke.mindustry.Vars.collisions;

public class Entities{
    public static final int maxLeafObjects = 4;
    private static final Array<EntityGroup<?>> groupArray = new Array<>();
    private static final IntMap<EntityGroup<?>> groups = new IntMap<>();
    private static final Rectangle viewport = new Rectangle();
    private static final boolean clip = true;
    private static int count = 0;

    public static void clear(){
        for(EntityGroup group : groupArray){
            group.clear();
        }
    }

    public static EntityGroup<?> getGroup(int id){
        return groups.get(id);
    }

    public static Array<EntityGroup<?>> getAllGroups(){
        return groupArray;
    }

    public static <T extends Entity> EntityGroup<T> addGroup(Class<T> type){
        return addGroup(type, true);
    }

    public static <T extends Entity> EntityGroup<T> addGroup(Class<T> type, boolean useTree){
        EntityGroup<T> group = new EntityGroup<>(type, useTree);
        groups.put(group.getID(), group);
        groupArray.add(group);
        return group;
    }

    public static void update(EntityGroup<?> group){
        group.updateEvents();

        if(group.useTree()){
            collisions.updatePhysics(group);
        }

        for(Entity e : group.all()){
            e.update();
        }
    }

    public static int countInBounds(EntityGroup<?> group){
        count = 0;
        draw(group, e -> true, e -> count++);
        return count;
    }

    public static void draw(EntityGroup<?> group){
        draw(group, e -> true);
    }

    public static <T extends DrawTrait> void draw(EntityGroup<?> group, Predicate<T> toDraw){
        draw(group, toDraw, DrawTrait::draw);
    }

    @SuppressWarnings("unchecked")
    public static <T extends DrawTrait> void draw(EntityGroup<?> group, Predicate<T> toDraw, Consumer<T> cons){
        if(clip){
            Camera cam = Core.camera;
            viewport.set(cam.position.x - cam.width / 2, cam.position.y - cam.height / 2, cam.width, cam.height);
        }

        for(Entity e : group.all()){
            if(!(e instanceof DrawTrait) || !toDraw.test((T)e) || !e.isAdded()) continue;
            DrawTrait draw = (DrawTrait)e;

            if(!clip || viewport.overlaps(draw.getX() - draw.drawSize()/2f, draw.getY() - draw.drawSize()/2f, draw.drawSize(), draw.drawSize())){
                cons.accept((T)e);
            }
        }
    }
}
