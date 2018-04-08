package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.graphics.Color;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.EffectEntity;
import io.anuke.ucore.function.EffectRenderer;
import io.anuke.ucore.util.Mathf;

public class GroundEffectEntity extends EffectEntity {
    private boolean once;

    public GroundEffectEntity(GroundEffect effect, Color color, float rotation) {
        super(effect, color, rotation);
    }

    @Override
    public void update(){
        GroundEffect effect = (GroundEffect)renderer;

        if(effect.isStatic) {
            time += Timers.delta();

            time = Mathf.clamp(time, 0, effect.staticLife);

            if (!once && time >= lifetime) {
                once = true;
                time = 0f;
            } else if (once && time >= effect.staticLife) {
                remove();
            }
        }else{
            super.update();
        }
    }

    @Override
    public void drawOver(){
        GroundEffect effect = (GroundEffect)renderer;

        if(once && effect.isStatic)
            Effects.renderEffect(id, renderer, color, lifetime, rotation, x, y);
        else if(!effect.isStatic)
            Effects.renderEffect(id, renderer, color, time, rotation, x, y);
    }

    public static class GroundEffect extends Effect{
        public final float staticLife;
        public final boolean isStatic;

        public GroundEffect(float life, float staticLife, EffectRenderer draw) {
            super(life, draw);
            this.staticLife = staticLife;
            this.isStatic = false;
        }

        public GroundEffect(boolean isStatic, float life, EffectRenderer draw) {
            super(life, draw);
            this.staticLife = 0f;
            this.isStatic = isStatic;
        }
    }
}
