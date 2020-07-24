package mindustry.entities.comp;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

//just a proof of concept
@Component
abstract class WaterMoveComp implements Posc, Velc, Hitboxc, Flyingc, Unitc{
    @Import float x, y, rotation;
    @Import UnitType type;

    private transient Trail tleft = new Trail(1), tright = new Trail(1);
    private transient Color trailColor = Blocks.water.mapColor.cpy().mul(1.5f);

    @Override
    public void update(){
        for(int i = 0; i < 2; i++){
            Trail t = i == 0 ? tleft : tright;
            t.length = type.trailLength;

            int sign = i == 0 ? -1 : 1;
            float cx = Angles.trnsx(rotation - 90, type.trailX * sign, type.trailY) + x, cy = Angles.trnsy(rotation - 90, type.trailX * sign, type.trailY) + y;
            t.update(cx, cy);
        }
    }

    public void draw(){
        float z = Draw.z();

        Draw.z(Layer.debris);

        Floor floor = floorOn();
        Color color = Tmp.c1.set(floor.mapColor).mul(1.5f);
        trailColor.lerp(color, Mathf.clamp(Time.delta * 0.04f));

        tleft.draw(trailColor, type.trailScl);
        tright.draw(trailColor, type.trailScl);

        Draw.z(z);
    }

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

