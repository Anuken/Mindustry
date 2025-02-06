package mindustry.type.unit;

import mindustry.world.meta.*;

public class TankUnitType extends ErekirUnitType{

    public TankUnitType(String name){
        super(name);

        squareShape = true;
        omniMovement = false;
        rotateMoveFirst = true;
        rotateSpeed = 1.3f;
        envDisabled = Env.none;
        speed = 0.8f;
    }

}
