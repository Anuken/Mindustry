package mindustry.world.meta;

import arc.Core;

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
    minutes,
    perSecond,
    perMinute,
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
