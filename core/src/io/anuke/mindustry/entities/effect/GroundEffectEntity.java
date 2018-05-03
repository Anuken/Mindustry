package io.anuke.mindustry.entities.effect;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.EffectEntity;
import io.anuke.ucore.function.EffectRenderer;
import io.anuke.ucore.util.Mathf;

public class GroundEffectEntity extends EffectEntity {
    private boolean once;

    @Override
    public void update(){
        GroundEffect effect = (GroundEffect)this.effect;

        if(effect.isStatic) {
            time += Timers.delta();

            time = Mathf.clamp(time, 0, effect.staticLife);

            if (!once && time >= lifetime) {
                once = true;
                time = 0f;
                Tile tile = Vars.world.tileWorld(x, y);
                if(tile != null && tile.floor().liquid){
                    remove();
                }
            } else if (once && time >= effect.staticLife) {
                remove();
            }
        }else{
            super.update();
        }
    }

    @Override
    public void draw(){
        GroundEffect effect = (GroundEffect)this.effect;

        if(once && effect.isStatic)
            Effects.renderEffect(id, effect, color, lifetime, rotation, x, y, data);
        else
            Effects.renderEffect(id, effect, color, time, rotation, x, y, data);
    }

    @Override
    public void reset() {
        super.reset();
        once = false;
    }

    public static class GroundEffect extends Effect{
        public final float staticLife;
        public final boolean isStatic;

        public GroundEffect(float life, float staticLife, EffectRenderer draw) {
            super(life, draw);
            this.staticLife = staticLife;
            this.isStatic = true;
        }

        public GroundEffect(boolean isStatic, float life, EffectRenderer draw) {
            super(life, draw);
            this.staticLife = 0f;
            this.isStatic = isStatic;
        }
    }
}
