package mindustry.type.unit;

import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.meta.*;

/** Config class for special Erekir unit properties. */
public class ErekirUnitType extends UnitType{

    public ErekirUnitType(String name){
        super(name);
        outlineColor = Pal.darkOutline;
        envDisabled = Env.space;
        researchCostMultiplier = 10f;
    }
}
