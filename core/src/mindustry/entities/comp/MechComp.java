package mindustry.entities.comp;

import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

@Component
abstract class MechComp implements Posc, Flyingc, Hitboxc, Unitc, Mechc, ElevationMovec{
    @SyncField(false) @SyncLocal float baseRotation;
    transient float walkTime;

    @Override
    public void update(){
        float len = deltaLen();
        baseRotation = Angles.moveToward(baseRotation, deltaAngle(), type().baseRotateSpeed * Mathf.clamp(len / type().speed));
        walkTime += Time.delta *len;
    }
}
