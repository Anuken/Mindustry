package mindustry.entities.def;

import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

@Component
abstract class GroundEffectComp implements Effectc, DrawLayerFloorOverc{

    @Override
    public void drawFloorOver(){
        draw();
    }
}
