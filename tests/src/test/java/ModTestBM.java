import mindustry.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import org.junit.jupiter.api.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

//grabs a betamindy release and makes sure it initializes correctly
//this mod was chosen because:
//- it is one of the top java mods on the browser
//- it uses a variety of mindustry classes
//- it is popular enough to cause significant amounts of crashes when something breaks
//- I have some familiarity with its codebase
public class ModTestBM extends GenericModTest{

    @Test
    public void begin(){
        //TODO broken as of 136+
        if(true) return;

        grabMod("https://github.com/sk7725/BetaMindy/releases/download/v0.955/BetaMindy.jar");

        checkExistence("betamindy");

        Block type = Vars.content.blocks().find(u -> u.name.equals("betamindy-piston"));
        assertNotNull(type, "A mod block must be loaded.");
        assertSame(type.buildVisibility, BuildVisibility.shown, "A mod block must be buildable.");

        world.loadMap(maps.loadInternalMap("groundZero"));
        Tile t = world.tile(3, 3);

        t.setBlock(type);

        //check for crash
        t.build.update();

        assertTrue(t.build.health > 0, "Block must be spawned and alive.");
        assertSame(t.build.block, type, "Block must be spawned and alive.");
    }

}
