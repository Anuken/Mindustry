package mindustry.type.unit;

import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.meta.*;

public class TankUnitType extends UnitType{

    public TankUnitType(String name){
        super(name);

        squareShape = true;
        omniMovement = false;
        rotateSpeed = 1.3f;
        envDisabled = Env.none;
        speed = 0.8f;
        outlineColor = Pal.darkOutline;
    }

}
