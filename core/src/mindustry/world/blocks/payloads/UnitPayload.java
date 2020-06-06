package mindustry.world.blocks.payloads;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

public class UnitPayload implements Payload{
    public Unitc unit;

    public UnitPayload(Unitc unit){
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
    public boolean dump(){
        //no client dumping
        if(Vars.net.client()) return true;

        //prevents stacking
        unit.vel().add(Mathf.range(0.5f), Mathf.range(0.5f));
        unit.add();

        return true;
    }

    @Override
    public void draw(){
        Drawf.shadow(unit.x(), unit.y(), 20);
        Draw.rect(unit.type().icon(Cicon.full), unit.x(), unit.y(), unit.rotation() - 90);
    }
}
