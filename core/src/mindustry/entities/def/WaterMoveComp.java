package mindustry.entities.def;

import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.gen.*;

import static mindustry.Vars.collisions;

//just a proof of concept
@Component
abstract class WaterMoveComp implements Posc, Velc, Hitboxc, Flyingc{
    transient float x, y;

    @Replace
    @Override
    public void move(float cx, float cy){
        if(isGrounded()){
            if(!EntityCollisions.waterSolid(tileX(), tileY())){
                collisions.move(this, cx, cy, EntityCollisions::waterSolid);
            }
        }else{
            x += cx;
            y += cy;
        }
    }

    @Replace
    @Override
    public boolean canDrown(){
        return false;
    }
}

