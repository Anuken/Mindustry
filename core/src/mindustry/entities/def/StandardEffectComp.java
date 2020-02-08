package mindustry.entities.def;

import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

@Component
abstract class StandardEffectComp implements Effectc, DrawLayerEffectsc{

    @Override
    public void drawEffects(){
        draw();
    }
}
