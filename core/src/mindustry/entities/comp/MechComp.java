package mindustry.entities.comp;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.environment.*;

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

        if(!headless && extendScl < lastExtend && base % 2f > 1f && !isFlying() && !inFogTo(player.team())){
            int side = -Mathf.sign(extend);
            float width = hitSize / 2f * side, length = type.mechStride * 1.35f;

            float cx = x + Angles.trnsx(baseRotation, length, width),
            cy = y + Angles.trnsy(baseRotation, length, width);

            if(type.stepShake > 0){
                Effect.shake(type.stepShake, type.stepShake, cx, cy);
            }

            if(type.mechStepParticles){
                Effect.floorDust(cx, cy, hitSize/8f);
            }
        }

        walkExtension = extendScl;
    }

    @Replace
    @Override
    public @Nullable Floor drownFloor(){
        //large mechs can only drown when all the nearby floors are deep
        if(hitSize >= 12 && canDrown()){
            for(Point2 p : Geometry.d8){
                Floor f = world.floorWorld(x + p.x * tilesize, y + p.y * tilesize);
                if(!f.isDeep()){
                    return null;
                }
            }
        }
        return canDrown() ? floorOn() : null;
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
    @Replace
    public void rotateMove(Vec2 vec){
        //mechs use baseRotation to rotate, not rotation.
        moveAt(Tmp.v2.trns(baseRotation, vec.len()));

        if(!vec.isZero()){
            baseRotation = Angles.moveToward(baseRotation, vec.angle(), type.rotateSpeed * Math.max(Time.delta, 1));
        }
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
