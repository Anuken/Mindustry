package mindustry.entities.comp;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

@Component
abstract class MechComp implements Posc, Flyingc, Hitboxc, Unitc, Mechc, ElevationMovec{
    @SyncField(false) @SyncLocal float baseRotation;
    transient float walkTime, walkExtension;
    transient private boolean walked;

    @Override
    public void update(){
        //trigger animation only when walking manually
        if(walked){
            float len = deltaLen();
            baseRotation = Angles.moveToward(baseRotation, deltaAngle(), type().baseRotateSpeed * Mathf.clamp(len / type().speed / Time.delta) * Time.delta);
            walkTime += len;
            walked = false;
        }
    }

    @Override
    public void moveAt(Vec2 vector, float acceleration){
        if(!vector.isZero()){
            walked = true;
        }
    }
}
