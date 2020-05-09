package mindustry.world.blocks.payloads;

import arc.graphics.g2d.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

public class UnitPayload implements Payload{
    public Unitc unit;

    public UnitPayload(Unitc unit){
        this.unit = unit;
    }

    @Override
    public boolean dump(float x, float y, float rotation){
        unit.set(x, y);
        unit.rotation(rotation);
        unit.add();

        return true;
    }

    @Override
    public void draw(float x, float y, float rotation){
        Drawf.shadow(x, y, 24);
        Draw.rect("pneumatic-drill", x, y, rotation);
        Drawf.shadow(x, y, 20);
        Draw.rect(unit.type().icon(Cicon.full), x, y, rotation - 90);
    }
}
