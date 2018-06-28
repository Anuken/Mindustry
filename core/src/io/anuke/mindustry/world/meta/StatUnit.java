package io.anuke.mindustry.world.meta;

import io.anuke.ucore.util.Bundles;

/**Defines a unit of measurement for block stats.*/
public enum StatUnit {
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
        return Bundles.get("text.unit." + name().toLowerCase());
    }
}
