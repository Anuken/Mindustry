package mindustry.entities.comp;

import arc.graphics.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.gen.*;

@EntityDef(value = {EffectStatec.class, Childc.class}, pooled = true, serialize = false)
@Component(base = true)
abstract class EffectStateComp implements Posc, Drawc, Timedc, Rotc, Childc{
    Color color = new Color(Color.white);
    Effect effect;
    Object data;

    @Override
    public void draw(){
        effect.render(id(), color, time(), rotation(), x(), y(), data);
    }

    @Replace
    public float clipSize(){
        return effect.size;
    }
}
