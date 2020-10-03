package mindustry.entities.comp;

import arc.graphics.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.gen.*;

@EntityDef(value = {EffectStatec.class, Childc.class}, pooled = true, serialize = false)
@Component(base = true)
abstract class EffectStateComp implements Posc, Drawc, Timedc, Rotc, Childc{
    @Import float time, lifetime, rotation, x, y;
    @Import int id;

    Color color = new Color(Color.white);
    Effect effect;
    Object data;

    @Override
    public void draw(){
        lifetime = effect.render(id, color, time, lifetime, rotation, x, y, data);
    }

    @Replace
    public float clipSize(){
        return effect.size;
    }
}
