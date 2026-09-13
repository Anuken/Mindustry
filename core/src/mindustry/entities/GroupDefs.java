package mindustry.entities;

import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

class GroupDefs<G>{
    @GroupDef(value = Entityc.class, exclude = {Unitc.class, PowerGraphUpdaterc.class, Bulletc.class, EffectStatec.class, Playerc.class}, update = true) G all;
    @GroupDef(value = EffectStatec.class) G effect;
    @GroupDef(value = Playerc.class, mapping = true) G player;
    @GroupDef(value = Bulletc.class, spatial = true, collide = true) G bullet;
    @GroupDef(value = Unitc.class, spatial = true, mapping = true) G unit;
    @GroupDef(value = Buildingc.class, update = true) G build;
    @GroupDef(value = Syncc.class, mapping = true) G sync;
    @GroupDef(value = Drawc.class) G draw;
    @GroupDef(value = WeatherStatec.class) G weather;
    @GroupDef(value = PowerGraphUpdaterc.class) G powerGraph;
}
