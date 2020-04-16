package mindustry.entities.def;

import arc.graphics.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.gen.*;

@Component
abstract class EffectComp implements Posc, Drawc, Timedc, Rotc, Childc{
    Color color = new Color(Color.white);
    Effect effect;
    Object data;

    void draw(){
        effect.render(id(), color, time(), rotation(), x(), y(), data);
    }

    @Override
    public float clipSize(){
        return effect.size;
    }
}
