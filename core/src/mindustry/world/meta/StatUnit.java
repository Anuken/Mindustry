package mindustry.world.meta;

import arc.*;

import java.util.*;

/**
 * Defines a unit of measurement for block stats.
 */
public class StatUnit{
    public static final StatUnit

    blocks = new StatUnit("blocks"),
    blocksSquared = new StatUnit("blocksSquared"),
    tilesSecond = new StatUnit("tilesSecond"),
    powerSecond = new StatUnit("powerSecond"),
    liquidSecond = new StatUnit("liquidSecond"),
    itemsSecond = new StatUnit("itemsSecond"),
    liquidUnits = new StatUnit("liquidUnits"),
    powerUnits = new StatUnit("powerUnits"),
    heatUnits = new StatUnit("heatUnits"),
    degrees = new StatUnit("degrees"),
    seconds = new StatUnit("seconds"),
    minutes = new StatUnit("minutes"),
    perSecond = new StatUnit("perSecond", false),
    perMinute = new StatUnit("perMinute", false),
    perShot = new StatUnit("perShot", false),
    timesSpeed = new StatUnit("timesSpeed", false),
    percent = new StatUnit("percent", false),
    shieldHealth = new StatUnit("shieldHealth"),
    none = new StatUnit("none"),
    items = new StatUnit("items");

    public final boolean space;
    public final String name;

    public StatUnit(String name, boolean space){
        this.name = name;
        this.space = space;
    }

    public StatUnit(String name){
        this(name, true);
    }

    public String localized(){
        if(this == none) return "";
        return Core.bundle.get("unit." + name.toLowerCase(Locale.ROOT));
    }
}
