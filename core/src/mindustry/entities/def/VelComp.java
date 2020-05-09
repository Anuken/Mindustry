package mindustry.entities.def;

import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

@Component
abstract class VelComp implements Posc{
    @Import float x, y;

    final Vec2 vel = new Vec2();
    transient float drag = 0f;

    //velocity needs to be called first, as it affects delta and lastPosition
    @MethodPriority(-1)
    @Override
    public void update(){
        move(vel.x * Time.delta(), vel.y * Time.delta());
        vel.scl(1f - drag * Time.delta());
    }

    boolean moving(){
        return !vel.isZero(0.001f);
    }

    void move(float cx, float cy){
        x += cx;
        y += cy;
    }
}
