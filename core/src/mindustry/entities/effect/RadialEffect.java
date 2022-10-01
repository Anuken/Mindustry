package mindustry.entities.effect;

import arc.math.*;
import mindustry.content.*;
import mindustry.entities.*;

/** Renders one particle effect repeatedly at specified angle intervals. */
public class RadialEffect extends Effect{
    public Effect effect = Fx.none;
    public float rotationSpacing = 90f, rotationOffset = 0f;
    public float lengthOffset = 0f;
    public int amount = 4;

    public RadialEffect(){
        clip = 100f;
    }

    public RadialEffect(Effect effect, int amount, float spacing, float lengthOffset){
        this();
        this.amount = amount;
        this.effect = effect;
        this.rotationSpacing = spacing;
        this.lengthOffset = lengthOffset;
    }

    @Override
    public void init(){
        effect.init();
        clip = Math.max(clip, effect.clip);
        lifetime = effect.lifetime;
    }

    @Override
    public void render(EffectContainer e){
        float x = e.x, y = e.y;

        e.rotation += rotationOffset;

        for(int i = 0; i < amount; i++){
            e.x = x + Angles.trnsx(e.rotation, lengthOffset);
            e.y = y + Angles.trnsy(e.rotation, lengthOffset);
            effect.render(e);
            e.rotation += rotationSpacing;
            e.id ++;
        }

        clip = Math.max(clip, effect.clip);
    }
}
