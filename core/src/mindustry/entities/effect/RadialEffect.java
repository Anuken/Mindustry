package mindustry.entities.effect;

import arc.graphics.*;
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
    public void create(float x, float y, float rotation, Color color, Object data){
        if(!shouldCreate()) return;

        rotation += rotationOffset;

        for(int i = 0; i < amount; i++){
            effect.create(x + Angles.trnsx(rotation, lengthOffset), y + Angles.trnsy(rotation, lengthOffset), rotation, color, data);
            rotation += rotationSpacing;
        }
    }
}
