import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.type.*;
import org.junit.jupiter.api.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

//grabs a version-locked Heavy Armaments Industries commit and makes sure it initializes correctly
//this mod was chosen because:
//- it is one of the top JS mods, based on stars
//- it contains both JS and JSON, which can be used to test compatibility of the two
//- it can be used server-side (unlike FactoryDustry, which is a client-side texture pack that cannot be tested here)
public class ModTestHAI extends GenericModTest{

    @Test
    public void begin(){
        //TODO broken as of 136+
        if(true) return;

        grabMod("https://github.com/Eschatologue/Heavy-Armaments-Industries/archive/d996e92dcf9a30a6acb7b3bfdfb6522dddc3804c.zip");
        checkExistence("heavy-armaments");

        UnitType type = Vars.content.units().find(u -> u.name.equals("heavy-armaments-t3A_copter"));
        assertNotNull(type, "A mod unit must be loaded.");
        assertTrue(type.weapons.size > 0, "A mod unit must have a weapon.");

        Vars.world.loadMap(maps.loadInternalMap("groundZero"));

        Unit unit = type.spawn(0, 0);

        //check for crash
        unit.update();

        assertTrue(unit.health > 0, "Unit must be spawned and alive.");
        assertTrue(Groups.unit.size() > 0, "Unit must be spawned and alive.");

        //just an extra sanity check
        Log.info("Modded units: @", Vars.content.units().select(u -> u.minfo.mod != null));
    }

}
