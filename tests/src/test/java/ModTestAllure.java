import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.type.*;
import org.junit.jupiter.api.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

//grabs a version-locked exotic-mod commit and makes sure its content is parsed correctly
//this mod was chosen because:
//- it is written solely in (h)json
//- it is probably the mod with the most json, and as such covers a lot of classes
//- it is popular enough in the mod browser
//- I am somewhat familiar with its files & the type of content it adds
public class ModTestAllure extends GenericModTest{

    @Test
    public void begin(){
        //As of September 5, the Allure repository has been deleted or made private. I do not know why.
        if(true) return;
        grabMod("https://github.com/LixieWulf/Allure/archive/c94e83e07ab918c0402b8f3897bba832e42432f0.zip");
        checkExistence("allure");

        UnitType type = Vars.content.unit("allure-0b11-exodus");
        assertNotNull(type, "A mod unit must be loaded.");
        assertTrue(type.weapons.size > 0, "A mod unit must have a weapon.");

        Vars.world.loadMap(maps.loadInternalMap("serpulo/groundZero"));

        Unit unit = type.spawn(0, 0);

        //check for crash
        unit.update();

        assertTrue(unit.health > 0, "Unit must be spawned and alive.");
        assertTrue(Groups.unit.size() > 0, "Unit must be spawned and alive.");

        //just an extra sanity check
        Log.info("Modded units: @", Vars.content.units().select(u -> u.minfo.mod != null));
    }

}
