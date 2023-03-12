package mindustry.entities.effect;

import mindustry.entities.*;

/**
 * Renders multiple particle effects in sequence.
 * Will not work correctly for effects that modify life dynamically.
 * Z layer of child effects is ignored.
 * */
public class SeqEffect extends Effect{
    public Effect[] effects = {};

    public SeqEffect(){
        clip = 100f;
    }

    public SeqEffect(Effect... effects){
        this();
        this.effects = effects;
    }

    @Override
    public void init(){
        lifetime = 0f;
        for(Effect f : effects){
            f.init();
            clip = Math.max(clip, f.clip);
            lifetime += f.lifetime;
        }
    }

    @Override
    public void render(EffectContainer e){
        var cont = e.inner();
        float life = e.time, sum = 0f;
        for(int i = 0; i < effects.length; i++){
            var fx = effects[i];
            if(life <= fx.lifetime + sum){
                cont.set(e.id + i, e.color, life - sum, fx.lifetime, e.rotation, e.x, e.y, e.data);
                fx.render(cont);
                clip = Math.max(clip, fx.clip);
                break;
            }
            sum += fx.lifetime;
        }
    }
}
