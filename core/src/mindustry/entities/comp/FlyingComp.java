package mindustry.entities.comp;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

@Component
abstract class FlyingComp implements Posc, Velc, Healthc, Hitboxc{
    private static final Vec2 tmp1 = new Vec2(), tmp2 = new Vec2();

    @Import float x, y, speedMultiplier, hitSize;
    @Import Vec2 vel;
    @Import UnitType type;

    @SyncLocal float elevation;
    private transient boolean wasFlying;
    transient boolean hovering;
    transient float drownTime;
    transient float splashTimer;
    transient @Nullable Floor lastDrownFloor;

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

    @Nullable Floor drownFloor(){
        return canDrown() ? floorOn() : null;
    }

    boolean emitWalkSound(){
        return true;
    }

    void landed(){

    }

    void wobble(){
        x += Mathf.sin(Time.time + (id() % 10) * 12, 25f, 0.05f) * Time.delta * elevation;
        y += Mathf.cos(Time.time + (id() % 10) * 12, 25f, 0.05f) * Time.delta * elevation;
    }

    void moveAt(Vec2 vector, float acceleration){
        Vec2 t = tmp1.set(vector); //target vector
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
                    Fx.unitLand.at(x, y, floorOn().isLiquid ? 1f : 0.5f, tileOn().floor().mapColor);
                }
            }

            wasFlying = isFlying();
        }

        if(!hovering && isGrounded()){
            if((splashTimer += Mathf.dst(deltaX(), deltaY())) >= (7f + hitSize()/8f)){
                floor.walkEffect.at(x, y, hitSize() / 8f, floor.mapColor);
                splashTimer = 0f;

                if(emitWalkSound()){
                    floor.walkSound.at(x, y, Mathf.random(floor.walkSoundPitchMin, floor.walkSoundPitchMax), floor.walkSoundVolume);
                }
            }
        }

        updateDrowning();
    }

    public void updateDrowning(){
        Floor floor = drownFloor();

        if(floor != null && floor.isLiquid && floor.drownTime > 0){
            lastDrownFloor = floor;
            drownTime += Time.delta / floor.drownTime / type.drownTimeMultiplier;
            if(Mathf.chanceDelta(0.05f)){
                floor.drownUpdateEffect.at(x, y, hitSize, floor.mapColor);
            }

            if(drownTime >= 0.999f && !net.client()){
                kill();
                Events.fire(new UnitDrownEvent(self()));
            }
        }else{
            drownTime -= Time.delta / 50f;
        }

        drownTime = Mathf.clamp(drownTime);
    }
}
