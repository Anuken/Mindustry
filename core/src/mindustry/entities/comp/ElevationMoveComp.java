package mindustry.entities.comp;

import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.entities.EntityCollisions.*;
import mindustry.gen.*;

@Component
abstract class ElevationMoveComp implements Velc, Posc, Flyingc, Hitboxc{
    @Import float x, y;

    @Replace
    @Override
    public SolidPred solidity(){
        return isFlying() ? null : EntityCollisions::solid;
    }

}
