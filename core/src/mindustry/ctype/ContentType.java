package mindustry.ctype;

import arc.util.*;
import mindustry.ai.*;
import mindustry.entities.bullet.*;
import mindustry.type.*;
import mindustry.world.*;

/** Do not rearrange, ever! */
public enum ContentType{
    item("items", Item.class),
    block("items", Block.class),
    mech_UNUSED,
    bullet("bullets", BulletType.class),
    liquid("liquids", Liquid.class),
    status("statuses", StatusEffect.class),
    unit("units", UnitType.class),
    weather("weather", Weather.class),
    effect_UNUSED,
    sector("sectors", SectorPreset.class),
    loadout_UNUSED,
    typeid_UNUSED,
    error,
    planet("planets", Planet.class),
    ammo_UNUSED(),
    team("teams", TeamEntry.class),
    unitCommand("unitCommands", UnitCommand.class),
    unitStance("unitStances", UnitStance.class);

    public static final ContentType[] all = values();

    public final @Nullable Class<? extends Content> contentClass;
    public final String folderName;

    ContentType(){
        this.contentClass = null;
        this.folderName = "unused";
    }

    ContentType(String folderName, Class<? extends Content> contentClass){
        this.contentClass = contentClass;
        this.folderName = folderName;
    }
}
