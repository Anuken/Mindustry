package mindustry.world.meta;

import arc.*;

/** A specific category for a stat. */
public enum StatCat{
    general,
    power,
    liquids,
    items,
    crafting,
    function,
    optional;

    public String localized(){
        return Core.bundle.get("category." + name());
    }
}
