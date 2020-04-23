package mindustry.entities.def;

import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;

@EntityDef(value = {Effectc.class, Childc.class}, pooled = true)
@Component
abstract class EffectComp implements Posc, Drawc, Timedc, Rotc, Childc{
    Color color = new Color(Color.white);
    Effect effect;
    Object data;

    @Override
    public void draw(){
        Draw.z(Layer.effect);
        effect.render(id(), color, time(), rotation(), x(), y(), data);
    }

    @Override
    public float clipSize(){
        return effect.size;
    }
}
