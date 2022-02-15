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
public class ModTestExotic extends GenericModTest{

    @Test
    public void begin(){
        //TODO broken as of 136+
        if(true) return;

        grabMod("https://github.com/BlueWolf3682/Exotic-Mod/archive/08c861398ac9c3d1292132f9a110e17e06294a90.zip");
        checkExistence("exotic-mod");

        UnitType type = Vars.content.units().find(u -> u.name.equals("exotic-mod-luminance"));
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
