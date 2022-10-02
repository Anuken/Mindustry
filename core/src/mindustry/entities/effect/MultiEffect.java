package mindustry.entities.effect;

import arc.graphics.*;
import mindustry.entities.*;

/** Renders multiple particle effects at once. */
public class MultiEffect extends Effect{
    public Effect[] effects = {};

    public MultiEffect(){
    }

    public MultiEffect(Effect... effects){
        this.effects = effects;
    }

    @Override
    public void create(float x, float y, float rotation, Color color, Object data){
        if(!shouldCreate()) return;

        for(var effect : effects){
            effect.create(x, y, rotation, color, data);
        }
    }
}
