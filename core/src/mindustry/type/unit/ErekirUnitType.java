package mindustry.type.unit;

import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.meta.*;

/** Config class for special Erekir unit properties. */
public class ErekirUnitType extends UnitType{

    public ErekirUnitType(String name){
        super(name);
        commandLimit = 0;
        outlineColor = Pal.darkOutline;
        envDisabled = Env.space;
        defaultAI = false;
        coreUnitDock = true;
    }
}
