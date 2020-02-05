package mindustry.entities.def;

import mindustry.annotations.Annotations.*;
import mindustry.entities.def.EntityComps.*;

public class EntityGroupDefs{

    @GroupDef(EntityComp.class)
    void all(){

    }

    @GroupDef(PlayerComp.class)
    void player(){

    }

    @GroupDef(value = UnitComp.class, spatial = true)
    void unit(){

    }

    @GroupDef(TileComp.class)
    void tile(){

    }

    @GroupDef(DrawComp.class)
    void drawer(){

    }

    @GroupDef(SyncComp.class)
    void sync(){

    }
}
