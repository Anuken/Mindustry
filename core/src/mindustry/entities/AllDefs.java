package mindustry.entities;

import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

class AllDefs{

    @GroupDef(Entityc.class)
    class all{

    }

    @GroupDef(Playerc.class)
    class player{

    }

    @GroupDef(value = Bulletc.class, spatial = true, collide = {unit.class})
    class bullet{

    }

    @GroupDef(value = Unitc.class, spatial = true, collide = {unit.class})
    class unit{

    }

    @GroupDef(Tilec.class)
    class tile{

    }

    @GroupDef(Syncc.class)
    class sync{

    }
}
