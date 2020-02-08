package mindustry.entities.def;

import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

@Component
abstract class VelComp implements Posc{
    transient float x, y;

    final Vec2 vel = new Vec2();
    float drag = 0f;

    @Override
    public void update(){
        move(vel.x, vel.y);
        vel.scl(1f - drag * Time.delta());
    }

    void move(float cx, float cy){
        x += cx;
        y += cy;
    }
}
