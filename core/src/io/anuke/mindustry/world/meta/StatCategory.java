package io.anuke.mindustry.world.meta;

import io.anuke.arc.Core;

/** A specific category for a stat. */
public enum StatCategory{
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
