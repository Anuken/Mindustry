package mindustry.entities.comp;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.net;

@Component
abstract class FlyingComp implements Posc, Velc, Healthc, Hitboxc{
    private static final Vec2 tmp1 = new Vec2(), tmp2 = new Vec2();

    @Import float x, y, speedMultiplier;
    @Import Vec2 vel;

    @SyncLocal float elevation;
    private transient boolean wasFlying;
    transient boolean hovering;
    transient float drownTime;
    transient float splashTimer;

    boolean checkTarget(boolean targetAir, boolean targetGround){
        return (isGrounded() && targetGround) || (isFlying() && targetAir);
    }

    boolean isGrounded(){
        return elevation < 0.001f;
    }

    boolean isFlying(){
        return elevation >= 0.09f;
    }

    boolean canDrown(){
        return isGrounded() && !hovering;
    }

    void landed(){

    }

    void wobble(){
        x += Mathf.sin(Time.time() + id() * 99, 25f, 0.05f) * Time.delta * elevation;
        y += Mathf.cos(Time.time() + id() * 99, 25f, 0.05f) * Time.delta * elevation;
    }

    void moveAt(Vec2 vector, float acceleration){
        Vec2 t = tmp1.set(vector).scl(floorSpeedMultiplier()); //target vector
        tmp2.set(t).sub(vel).limit(acceleration * vector.len() * Time.delta); //delta vector
        vel.add(tmp2);
    }

    float floorSpeedMultiplier(){
        Floor on = isFlying() || hovering ? Blocks.air.asFloor() : floorOn();
        return on.speedMultiplier * speedMultiplier;
    }

    @Override
    public void update(){
        Floor floor = floorOn();

        if(isFlying() != wasFlying){
            if(wasFlying){
                if(tileOn() != null){
                    Fx.unitLand.at(x, y, floorOn().isLiquid ? 1f : 0.5f, floorOn().mapColor);
                }
            }

            wasFlying = isFlying();
        }

        if(!hovering && isGrounded() && floor.isLiquid){
            if((splashTimer += Mathf.dst(deltaX(), deltaY())) >= (7f + hitSize()/8f)){
                floor.walkEffect.at(x, y, hitSize() / 8f, floor.mapColor);
                splashTimer = 0f;
            }
        }

        if(canDrown() && floor.isLiquid && floor.drownTime > 0){
            drownTime += Time.delta * 1f / floor.drownTime;
            drownTime = Mathf.clamp(drownTime);
            if(Mathf.chanceDelta(0.05f)){
                floor.drownUpdateEffect.at(x, y, 1f, floor.mapColor);
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
