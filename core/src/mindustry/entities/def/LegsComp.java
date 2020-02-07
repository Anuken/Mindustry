package mindustry.entities.def;

import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

@Component
abstract class LegsComp implements Posc, Flyingc, Hitboxc, DrawLayerGroundUnderc, Unitc, Legsc{
    transient float x, y;

    float baseRotation, walkTime;

    @Override
    public void drawGroundUnder(){
        type().drawLegs(this);
    }
}
