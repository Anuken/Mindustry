package mindustry.ctype;

import arc.util.*;
import mindustry.entities.bullet.*;
import mindustry.type.*;
import mindustry.world.*;

/** Do not rearrange, ever! */
public enum ContentType{
    item(Item.class),
    block(Block.class),
    mech_UNUSED(null),
    bullet(BulletType.class),
    liquid(Liquid.class),
    status(StatusEffect.class),
    unit(UnitType.class),
    weather(Weather.class),
    effect_UNUSED(null),
    sector(SectorPreset.class),
    loadout_UNUSED(null),
    typeid_UNUSED(null),
    error(null),
    planet(Planet.class),
    ammo_UNUSED(null),
    team(TeamEntry.class);

    public static final ContentType[] all = values();

    public final @Nullable Class<? extends Content> contentClass;

    ContentType(Class<? extends Content> contentClass){
        this.contentClass = contentClass;
    }
}
