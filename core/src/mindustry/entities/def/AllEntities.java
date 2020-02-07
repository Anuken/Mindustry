package mindustry.entities.def;

import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

class AllEntities{

    @EntityDef(value = {Bulletc.class, Velc.class, Timedc.class}, pooled = true)
    class BulletDef{}

    @EntityDef(value = {Tilec.class}, isFinal = false)
    class TileDef{}

    @EntityDef(value = {Effectc.class}, pooled = true)
    class EffectDef{}

    @EntityDef({Decalc.class})
    class DecalDef{}

    @EntityDef({Playerc.class})
    class PlayerDef{}

    @EntityDef({Unitc.class})
    class GenericUnitDef{}

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
