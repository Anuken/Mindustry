package mindustry.world.blocks.payloads;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

public class UnitPayload implements Payload{
    public static final float deactiveDuration = 40f;

    public Unit unit;
    public float deactiveTime = 0f;

    public UnitPayload(Unit unit){
        this.unit = unit;
    }

    @Override
    public void write(Writes write){
        write.b(payloadUnit);
        write.b(unit.classId());
        unit.write(write);
    }

    @Override
    public void set(float x, float y, float rotation){
        unit.set(x, y);
        unit.rotation(rotation);
    }

    @Override
    public float size(){
        return unit.hitSize;
    }

    @Override
    public boolean dump(){
        if(!Units.canCreate(unit.team, unit.type())){
            deactiveTime = 1f;
            return false;
        }

        //naval units need water.
        if(unit instanceof WaterMovec){
            int tx = unit.tileX(), ty = unit.tileY();
            boolean nearEmpty = !EntityCollisions.waterSolid(tx, ty);
            for(Point2 p : Geometry.d4){
                nearEmpty |= !EntityCollisions.waterSolid(tx + p.x, ty + p.y);
            }

            //cannot dump on dry land
            if(!nearEmpty) return false;
        }

        //no client dumping
        if(Vars.net.client()) return true;

        //prevents stacking
        unit.vel.add(Mathf.range(0.5f), Mathf.range(0.5f));
        unit.add();

        return true;
    }

    @Override
    public void draw(){
        Drawf.shadow(unit.x, unit.y, 20);
        Draw.rect(unit.type().icon(Cicon.full), unit.x, unit.y, unit.rotation - 90);

        //draw warning
        if(deactiveTime > 0){
            Draw.color(Color.scarlet);
            Draw.alpha(0.8f * Interp.exp5Out.apply(deactiveTime));

            float size = 8f;
            Draw.rect(Icon.warning.getRegion(), unit.x, unit.y, size, size);

            Draw.reset();

            deactiveTime = Math.max(deactiveTime - Time.delta/deactiveDuration, 0f);
        }
    }
}
