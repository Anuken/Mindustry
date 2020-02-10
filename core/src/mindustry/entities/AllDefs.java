package mindustry.entities;

import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

class AllDefs{

    @GroupDef(Entityc.class)
    void all(){

    }

    @GroupDef(Playerc.class)
    void player(){

    }

    @GroupDef(value = Unitc.class, spatial = true)
    void unit(){

    }

    @GroupDef(Tilec.class)
    void tile(){

    }

    @GroupDef(Syncc.class)
    void sync(){

    }
}
