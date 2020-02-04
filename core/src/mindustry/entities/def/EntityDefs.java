package mindustry.entities.def;

import mindustry.annotations.Annotations.*;
import mindustry.entities.def.EntityComps.*;

class EntityDefs{

    @EntityDef({BulletComp.class, VelComp.class, TimedComp.class})
    class BulletDef{}

    @EntityDef({TileComp.class})
    class TileDef{}

    @EntityDef({EffectComp.class})
    class EffectDef{}

    @EntityDef({DecalComp.class})
    class DecalDef{}
}
