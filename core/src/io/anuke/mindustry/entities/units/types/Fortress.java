package io.anuke.mindustry.entities.units.types;

import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.units.GroundUnit;

public class Fortress extends GroundUnit{

    @Override
    protected void patrol(){
        if(Units.invalidateTarget(target, this)){
            super.patrol();
        }
    }

    @Override
    protected void moveToCore(){
        if(Units.invalidateTarget(target, this)){
            super.moveToCore();
        }
    }
}
