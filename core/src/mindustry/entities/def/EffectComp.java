package mindustry.entities.def;

import arc.graphics.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.gen.*;

@EntityDef(value = {Effectc.class, Childc.class}, pooled = true, serialize = false)
@Component
abstract class EffectComp implements Posc, Drawc, Timedc, Rotc, Childc{
    Color color = new Color(Color.white);
    Effect effect;
    Object data;

    @Override
    public void draw(){
        effect.render(id(), color, time(), rotation(), x(), y(), data);
    }

    @Override
    public float clipSize(){
        return effect.size;
    }
}
