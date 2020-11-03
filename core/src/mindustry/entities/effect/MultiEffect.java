package mindustry.entities.effect;

import mindustry.entities.*;

/** Renders multiple particle effects at once. */
public class MultiEffect extends Effect{
    public Effect[] effects = {};

    public MultiEffect(){
        clip = 100f;
    }

    public MultiEffect(Effect... effects){
        this();
        this.effects = effects;
    }

    @Override
    public void init(){
        for(Effect f : effects){
            clip = Math.max(clip, f.clip);
        }
    }

    @Override
    public void render(EffectContainer e){
        for(Effect f : effects){
            e.scaled(f.lifetime, f::render);
            clip = Math.max(clip, f.clip);
        }
    }
}
