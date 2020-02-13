package mindustry.entities.def;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.net;

@Component
abstract class FlyingComp implements Posc, Velc, Healthc, Hitboxc{
    @Import float x, y;
    @Import Vec2 vel;

    float elevation;
    float drownTime;
    transient float splashTimer;

    boolean isGrounded(){
        return elevation < 0.001f;
    }

    boolean isFlying(){
        return elevation >= 0.001f;
    }

    boolean canDrown(){
        return isGrounded();
    }

    void moveAt(Vec2 vector){
        Vec2 t = Tmp.v3.set(vector).scl(floorSpeedMultiplier()); //target vector
        float mag = Tmp.v3.len();
        vel.x = Mathf.approach(vel.x, t.x, mag);
        vel.y = Mathf.approach(vel.y, t.y, mag);
    }

    float floorSpeedMultiplier(){
        Floor on = isFlying() ? Blocks.air.asFloor() : floorOn();
        return on.speedMultiplier;
    }

    @Override
    public void update(){
        Floor floor = floorOn();

        if(isGrounded() && floor.isLiquid){
            if((splashTimer += Mathf.dst(deltaX(), deltaY())) >= 7f){
                floor.walkEffect.at(x, y, 0, floor.color);
                splashTimer = 0f;
            }
        }

        if(canDrown() && floor.isLiquid && floor.drownTime > 0){
            drownTime += Time.delta() * 1f / floor.drownTime;
            drownTime = Mathf.clamp(drownTime);
            if(Mathf.chance(Time.delta() * 0.05f)){
                floor.drownUpdateEffect.at(x, y, 0f, floor.color);
            }

            //TODO is the netClient check necessary?
            if(drownTime >= 0.999f && !net.client()){
                kill();
                //TODO drown event!
            }
        }else{
            drownTime = Mathf.lerpDelta(drownTime, 0f, 0.03f);
        }
    }
}
