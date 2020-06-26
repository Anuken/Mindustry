package mindustry.entities;

import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

class GroupDefs<G>{
    @GroupDef(value = Entityc.class, mapping = true) G all;
    @GroupDef(value = Playerc.class, mapping = true) G player;
    @GroupDef(value = Bulletc.class, spatial = true, collide = true) G bullet;
    @GroupDef(value = Unitc.class, spatial = true, mapping = true) G unit;
    @GroupDef(value = Buildingc.class) G tile;
    @GroupDef(value = Syncc.class, mapping = true) G sync;
    @GroupDef(value = Drawc.class) G draw;
    @GroupDef(value = Weatherc.class) G weather;
}
