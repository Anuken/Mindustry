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
            lifetime = Math.max(lifetime, f.lifetime);
        }
    }

    @Override
    public void render(EffectContainer e){
        int index = 0;
        for(Effect f : effects){
            int i = ++index;
            e.scaled(f.lifetime, cont -> {
                cont.id = e.id + i;
                f.render(cont);
            });
            clip = Math.max(clip, f.clip);
        }
    }
}
