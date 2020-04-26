package mindustry.entities;

import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

class AllDefs{

    @GroupDef(value = Entityc.class, mapping = true)
    class all{

    }

    @GroupDef(value = Playerc.class, mapping = true)
    class player{

    }

    @GroupDef(value = Bulletc.class, spatial = true, collide = {unit.class})
    class bullet{

    }

    @GroupDef(value = Unitc.class, spatial = true, collide = {unit.class}, mapping = true)
    class unit{

    }

    @GroupDef(Tilec.class)
    class tile{

    }

    @GroupDef(value = Syncc.class, mapping = true)
    class sync{

    }

    @GroupDef(Drawc.class)
    class draw{

    }
}
