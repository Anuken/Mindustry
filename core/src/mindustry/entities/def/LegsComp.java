package mindustry.entities.def;

import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

@Component
abstract class LegsComp implements Posc, Flyingc, Hitboxc, DrawLayerGroundUnderc, Unitc, Legsc, ElevationMovec{
    @Import float x, y;

    float baseRotation;
    transient float walkTime;

    @Override
    public void update(){
        float len = vel().len();
        baseRotation = Angles.moveToward(baseRotation, vel().angle(), type().baseRotateSpeed * Mathf.clamp(len / type().speed));
        walkTime += Time.delta()*len/1f;
    }

    @Override
    public void drawGroundUnder(){
        type().drawLegs(this);
    }
}
