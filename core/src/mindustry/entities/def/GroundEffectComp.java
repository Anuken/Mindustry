package mindustry.entities.def;

import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

@EntityDef(value = {GroundEffectc.class, Childc.class}, pooled = true)
@Component
abstract class GroundEffectComp implements Effectc, DrawLayerFloorOverc{

    @Override
    public void drawFloorOver(){
        draw();
    }
}
