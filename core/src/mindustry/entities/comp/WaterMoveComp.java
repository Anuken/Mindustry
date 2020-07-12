package mindustry.entities.comp;

import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.collisions;

//just a proof of concept
@Component
abstract class WaterMoveComp implements Posc, Velc, Hitboxc, Flyingc{
    @Import float x, y;

    @Replace
    @Override
    public void move(float cx, float cy){
        if(isGrounded()){
            collisions.moveCheck(this, cx, cy, EntityCollisions::waterSolid);
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

    @Replace
    public float floorSpeedMultiplier(){
        Floor on = isFlying() ? Blocks.air.asFloor() : floorOn();
        return on.isDeep() ? 1.3f : 1f;
    }
}

