package io.anuke.mindustry.entities;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.function.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.math.geom.*;
import io.anuke.mindustry.entities.traits.*;

public class Entities{
    private final Array<EntityGroup<?>> groupArray = new Array<>();
    private final Rectangle viewport = new Rectangle();
    private final boolean clip = true;
    private int count = 0;

    public void clear(){
        for(EntityGroup group : groupArray){
            group.clear();
        }
    }

    public EntityGroup<?> getGroup(int id){
        return groupArray.get(id);
    }

    public Array<EntityGroup<?>> getAllGroups(){
        return groupArray;
    }

    public <T extends Entity> EntityGroup<T> addGroup(Class<T> type){
        return addGroup(type, true);
    }

    public <T extends Entity> EntityGroup<T> addGroup(Class<T> type, boolean useTree){
        EntityGroup<T> group = new EntityGroup<>(groupArray.size, type, useTree);
        groupArray.add(group);
        return group;
    }

    public int countInBounds(EntityGroup<?> group){
        count = 0;
        draw(group, e -> true, e -> count++);
        return count;
    }

    public void draw(EntityGroup<?> group){
        draw(group, e -> true);
    }

    public <T extends DrawTrait> void draw(EntityGroup<?> group, Predicate<T> toDraw){
        draw(group, toDraw, DrawTrait::draw);
    }

    @SuppressWarnings("unchecked")
    public <T extends DrawTrait> void draw(EntityGroup<?> group, Predicate<T> toDraw, Consumer<T> cons){
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
