package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.graphics.Color;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.EffectEntity;
import io.anuke.ucore.function.EffectRenderer;
import io.anuke.ucore.util.Mathf;

public class StaticEffectEntity extends EffectEntity {
    private boolean once;

    public StaticEffectEntity(StaticEffect effect, Color color, float rotation) {
        super(effect, color, rotation);
    }

    @Override
    public void update(){
        time += Timers.delta();

        time = Mathf.clamp(time, 0, ((StaticEffect)renderer).staticLife);

        if(!once && time >= lifetime){
            once = true;
            time = 0f;
        }else if(once && time >= ((StaticEffect)renderer).staticLife){
            remove();
        }
    }

    @Override
    public void drawOver(){
        if(once) Effects.renderEffect(id, renderer, color, once ? lifetime : time, rotation, x, y);
    }

    public static class StaticEffect extends Effect{
        public final float staticLife;

        public StaticEffect(float life, float staticLife, EffectRenderer draw) {
            super(life, draw);
            this.staticLife = staticLife;
        }
    }
}
