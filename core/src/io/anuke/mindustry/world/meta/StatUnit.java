package io.anuke.mindustry.world.meta;

import io.anuke.arc.util.Bundles;

import java.util.Locale;

/**
 * Defines a unit of measurement for block stats.
 */
public enum StatUnit{
    blocks,
    powerSecond,
    liquidSecond,
    itemsSecond,
    pixelsSecond,
    liquidUnits,
    powerUnits,
    degrees,
    seconds,
    none,
    items;

    public String localized(){
        if(this == none) return "";
        return Core.bundle.get("text.unit." + name().toLowerCase(Locale.ROOT));
    }
}
