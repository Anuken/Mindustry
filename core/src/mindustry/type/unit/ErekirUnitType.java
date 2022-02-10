package mindustry.type.unit;

import mindustry.ai.types.*;
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
        coreUnitDock = true;
        unitBasedDefaultController = u -> !playerControllable || u.team.isAI() ? defaultController.get() : new CommandAI();
    }
}
