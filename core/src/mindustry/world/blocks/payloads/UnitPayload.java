package mindustry.world.blocks.payloads;

import arc.graphics.g2d.*;
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
    public void set(float x, float y){
        unit.set(x, y);
    }

    @Override
    public boolean dump(float x, float y, float rotation){
        //no client dumping
        if(Vars.net.client()) return true;

        unit.set(x, y);
        unit.rotation(rotation);
        unit.add();

        return true;
    }

    @Override
    public void draw(float x, float y, float rotation){
       // Drawf.shadow(x, y, 24);
        //Draw.rect("pneumatic-drill", x, y, rotation);
        Drawf.shadow(x, y, 20);
        Draw.rect(unit.type().icon(Cicon.full), x, y, rotation - 90);
    }
}
