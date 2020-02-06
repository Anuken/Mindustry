package mindustry.entities.def;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.net;

@Component
abstract class FlyingComp implements Posc, Velc, Healthc{
    transient float x, y;
    transient Vec2 vel;

    float elevation;
    float drownTime;

    boolean isGrounded(){
        return elevation < 0.001f;
    }

    @Override
    public void update(){
        Floor floor = floorOn();

        if(isGrounded() && floor.isLiquid && vel.len2() > 0.4f*0.4f && Mathf.chance((vel.len2() * floor.speedMultiplier) * 0.03f * Time.delta())){
            floor.walkEffect.at(x, y, 0, floor.color);
        }

        if(isGrounded() && floor.isLiquid && floor.drownTime > 0){
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
