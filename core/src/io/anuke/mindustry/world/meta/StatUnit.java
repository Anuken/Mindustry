package io.anuke.mindustry.world.meta;

import io.anuke.arc.Core;

import java.util.Locale;

/**
 * Defines a unit of measurement for block stats.
 */
public enum StatUnit{
    blocks,
    powerSecond,
    liquidSecond,
    itemsSecond,
    liquidUnits,
    powerUnits,
    degrees,
    seconds,
    perSecond,
    timesSpeed(false),
    percent(false),
    none,
    items;

    public final boolean space;

    StatUnit(boolean space){
        this.space = space;
    }

    StatUnit(){
        this(true);
    }

    public String localized(){
        if(this == none) return "";
        return Core.bundle.get("unit." + name().toLowerCase(Locale.ROOT));
    }
}
