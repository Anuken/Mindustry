package mindustry.world.meta;

import arc.*;

/** A specific category for a stat. */
public enum StatCat{
    general,
    power,
    liquids,
    items,
    crafting,
    shooting,
    optional;

    public String localized(){
        return Core.bundle.get("category." + name());
    }
}
