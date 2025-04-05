package mindustry.entities.comp;

import mindustry.annotations.Annotations.*;
import mindustry.async.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

@Component
abstract class UnderwaterMoveComp implements WaterMovec{
    @Import UnitType type;

    @MethodPriority(10f)
    @Replace
    public void draw(){
        //TODO draw status effects?

        Drawf.underwater(() -> {
            type.draw(self());
        });
    }

    @Override
    public int collisionLayer(){
        return PhysicsProcess.layerUnderwater;
    }

    @Override
    public boolean hittable(){
        return false && type.hittable(self());
    }

    @Override
    public boolean targetable(Team targeter){
        return false && type.targetable(self(), targeter);
    }
}

