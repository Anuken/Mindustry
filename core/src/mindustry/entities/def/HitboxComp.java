package mindustry.entities.def;

import arc.math.geom.*;
import arc.math.geom.QuadTree.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

@Component
abstract class HitboxComp implements Posc, QuadTreeObject{
    transient float x, y;

    float hitSize;
    float lastX, lastY;

    @Override
    public void update(){

    }

    void updateLastPosition(){
        lastX = x;
        lastY = y;
    }

    void collision(Hitboxc other, float x, float y){

    }

    float deltaX(){
        return x - lastX;
    }

    float deltaY(){
        return y - lastY;
    }

    boolean collides(Hitboxc other){
        return Intersector.overlapsRect(x - hitSize/2f, y - hitSize/2f, hitSize, hitSize,
        other.x() - other.hitSize()/2f, other.y() - other.hitSize()/2f, other.hitSize(), other.hitSize());
    }

    @Override
    public void hitbox(Rect rect){
        rect.setCentered(x, y, hitSize, hitSize);
    }

    public void hitboxTile(Rect rect){
        float scale = 0.6f;
        rect.setCentered(x, y, hitSize * scale, hitSize * scale);
    }
}
