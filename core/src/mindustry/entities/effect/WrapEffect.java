package mindustry.entities.effect;

import arc.graphics.*;
import mindustry.content.*;
import mindustry.entities.*;

/** Wraps an effect with some parameters. */
public class WrapEffect extends Effect{
    public Effect effect = Fx.none;
    public Color color = Color.white.cpy();
    public float rotation = Float.NaN;

    public WrapEffect(){
    }

    public WrapEffect(Effect effect, Color color){
        this.effect = effect;
        this.color = color;
    }

    public WrapEffect(Effect effect, Color color, float rotation){
        this.effect = effect;
        this.color = color;
        this.rotation = rotation;
    }

    @Override
    public void init(){
        effect.init();
        clip = effect.clip;
        lifetime = effect.lifetime;
    }

    @Override
    public void render(EffectContainer e){
    }

    @Override
    public void create(float x, float y, float rotation, Color color, Object data){
        effect.create(x, y, this.rotation, this.color, data);
    }
}
