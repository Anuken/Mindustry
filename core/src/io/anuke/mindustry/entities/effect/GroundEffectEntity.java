package io.anuke.mindustry.entities.effect;

import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.Effects.Effect;
import io.anuke.mindustry.entities.Effects.EffectRenderer;
import io.anuke.mindustry.entities.type.EffectEntity;
import io.anuke.mindustry.world.Tile;

/**
 * A ground effect contains an effect that is rendered on the ground layer as opposed to the top layer.
 */
public class GroundEffectEntity extends EffectEntity{
    private boolean once;

    @Override
    public void update(){
        GroundEffect effect = (GroundEffect)this.effect;

        if(effect.isStatic){
            time += Time.delta();

            time = Mathf.clamp(time, 0, effect.staticLife);

            if(!once && time >= lifetime()){
                once = true;
                time = 0f;
                Tile tile = Vars.world.tileWorld(x, y);
                if(tile != null && tile.floor().isLiquid){
                    remove();
                }
            }else if(once && time >= effect.staticLife){
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
            Effects.renderEffect(id, effect, color, lifetime(), rotation, x, y, data);
        else
            Effects.renderEffect(id, effect, color, time, rotation, x, y, data);
    }

    @Override
    public void reset(){
        super.reset();
        once = false;
    }

    /**
     * An effect that is rendered on the ground layer as opposed to the top layer.
     */
    public static class GroundEffect extends Effect{
        /**
         * How long this effect stays on the ground when static.
         */
        public final float staticLife;
        /**
         * If true, this effect will stop and lie on the ground for a specific duration,
         * after its initial lifetime is over.
         */
        public final boolean isStatic;

        public GroundEffect(float life, float staticLife, EffectRenderer draw){
            super(life, draw);
            this.staticLife = staticLife;
            this.isStatic = true;
        }

        public GroundEffect(boolean isStatic, float life, EffectRenderer draw){
            super(life, draw);
            this.staticLife = 0f;
            this.isStatic = isStatic;
        }

        public GroundEffect(float life, EffectRenderer draw){
            super(life, draw);
            this.staticLife = 0f;
            this.isStatic = false;
        }
    }
}
