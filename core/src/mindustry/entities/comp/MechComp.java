package mindustry.entities.comp;

import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

@Component
abstract class MechComp implements Posc, Flyingc, Hitboxc, Unitc, Mechc, ElevationMovec{
    @Import float x, y, hitSize;
    @Import UnitType type;

    @SyncField(false) @SyncLocal float baseRotation;
    transient float walkTime, walkExtension;
    transient private boolean walked;

    @Override
    public void update(){
        //trigger animation only when walking manually
        if(walked || net.client()){
            float len = deltaLen();
            baseRotation = Angles.moveToward(baseRotation, deltaAngle(), type().baseRotateSpeed * Mathf.clamp(len / type().speed / Time.delta) * Time.delta);
            walkTime += len;
            walked = false;
        }

        //update mech effects
        float extend = walkExtend(false);
        float base = walkExtend(true);
        float extendScl = base % 1f;

        float lastExtend = walkExtension;

        if(extendScl < lastExtend && base % 2f > 1f && !isFlying()){
            int side = -Mathf.sign(extend);
            float width = hitSize / 2f * side, length = type.mechStride * 1.35f;

            float cx = x + Angles.trnsx(baseRotation, length, width),
            cy = y + Angles.trnsy(baseRotation, length, width);

            if(type.mechStepShake > 0){
                Effect.shake(type.mechStepShake, type.mechStepShake, cx, cy);
            }

            if(type.mechStepParticles){
                Tile tile = world.tileWorld(cx, cy);
                if(tile != null){
                    Color color = tile.floor().mapColor;
                    Fx.unitLand.at(cx, cy, hitSize/8f, color);
                }
            }
        }

        walkExtension = extendScl;
    }

    public float walkExtend(boolean scaled){

        //now ranges from -maxExtension to maxExtension*3
        float raw = walkTime % (type.mechStride * 4);

        if(scaled) return raw / type.mechStride;

        if(raw > type.mechStride*3) raw = raw - type.mechStride * 4;
        else if(raw > type.mechStride*2) raw = type.mechStride * 2 - raw;
        else if(raw > type.mechStride) raw = type.mechStride * 2 - raw;

        return raw;
    }

    @Override
    public void moveAt(Vec2 vector, float acceleration){
        //mark walking state when moving in a controlled manner
        if(!vector.isZero()){
            walked = true;
        }
    }

    @Override
    public void approach(Vec2 vector){
        //mark walking state when moving in a controlled manner
        if(!vector.isZero(0.001f)){
            walked = true;
        }
    }
}
