package mindustry.entities.def;

import mindustry.annotations.Annotations.*;
import mindustry.entities.def.EntityComps.*;

public class EntityGroupDefs{

    @GroupDef(PlayerComp.class)
    void player(){

    }

    @GroupDef(UnitComp.class)
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
