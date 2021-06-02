package mindustry.world.blocks.payloads;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.entities.EntityCollisions.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;

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
        unit.rotation = rotation;
    }

    @Override
    public float x(){
        return unit.x;
    }

    @Override
    public float y(){
        return unit.y;
    }

    @Override
    public float rotation(){
        return unit.rotation;
    }

    @Override
    public float size(){
        return unit.hitSize;
    }

    @Override
    public boolean dump(){
        //TODO should not happen
        if(unit.type == null) return true;

        if(!Units.canCreate(unit.team, unit.type)){
            deactiveTime = 1f;
            return false;
        }

        //check if unit can be dumped here
        SolidPred solid = unit.solidity();
        if(solid != null){
            Tmp.v1.trns(unit.rotation, 1f);

            int tx = World.toTile(unit.x + Tmp.v1.x), ty = World.toTile(unit.y + Tmp.v1.y);

            //cannot dump on solid blocks
            if(solid.solid(tx, ty)) return false;
        }

        //cannnot dump when there's a lot of overlap going on
        if(!unit.type.flying && Units.count(unit.x, unit.y, unit.physicSize(), o -> o.isGrounded() && (o.type.allowLegStep == unit.type.allowLegStep)) > 0){
            return false;
        }

        //no client dumping
        if(Vars.net.client()) return true;

        //prevents stacking
        unit.vel.add(Mathf.range(0.5f), Mathf.range(0.5f));
        unit.add();
        Events.fire(new UnitUnloadEvent(unit));

        return true;
    }

    @Override
    public void draw(){
        //TODO should not happen
        if(unit.type == null) return;

        unit.type.drawSoftShadow(unit);
        Draw.rect(unit.type.fullIcon, unit.x, unit.y, unit.rotation - 90);
        unit.type.drawCell(unit);

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

    @Override
    public TextureRegion icon(){
        return unit.type.fullIcon;
    }
}
