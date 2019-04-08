package io.anuke.mindustry.entities;

import io.anuke.arc.Core;
import io.anuke.arc.function.Consumer;
import io.anuke.arc.function.Predicate;
import io.anuke.arc.graphics.Camera;
import io.anuke.arc.math.geom.Rectangle;
import io.anuke.mindustry.entities.traits.DrawTrait;
import io.anuke.mindustry.entities.traits.Entity;

public class EntityDraw{
    private static final Rectangle viewport = new Rectangle();
    private static final Rectangle rect = new Rectangle();
    private static boolean clip = true;
    private static int count = 0;

    public static void setClip(boolean clip){
        EntityDraw.clip = clip;
    }

    public static int countInBounds(EntityGroup<?> group){
        count = 0;
        drawWith(group, e -> true, e -> count++);
        return count;
    }

    public static void draw(){
        draw(Entities.defaultGroup());
    }

    public static void draw(EntityGroup<?> group){
        draw(group, e -> true);
    }

    public static <T extends DrawTrait> void draw(EntityGroup<?> group, Predicate<T> toDraw){
        drawWith(group, toDraw, DrawTrait::draw);
    }

    @SuppressWarnings("unchecked")
    public static <T extends DrawTrait> void drawWith(EntityGroup<?> group, Predicate<T> toDraw, Consumer<T> cons){
        if(clip){
            Camera cam = Core.camera;
            viewport.set(cam.position.x - cam.width / 2, cam.position.y - cam.height / 2, cam.width, cam.height);
        }

        for(Entity e : group.all()){
            if(!(e instanceof DrawTrait) || !toDraw.test((T)e) || !e.isAdded()) continue;

            if(!clip || rect.setSize(((DrawTrait)e).drawSize()).setCenter(e.getX(), e.getY()).overlaps(viewport)){
                cons.accept((T)e);
            }
        }
    }
}
