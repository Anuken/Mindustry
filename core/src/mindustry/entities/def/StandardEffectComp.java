package mindustry.entities.def;

import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

@EntityDef(value = {StandardEffectc.class, Childc.class}, pooled = true)
@Component
abstract class StandardEffectComp implements Effectc, DrawLayerEffectsc{

    @Override
    public void drawEffects(){
        draw();
    }
}
