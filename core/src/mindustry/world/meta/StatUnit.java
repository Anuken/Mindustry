package mindustry.world.meta;

import arc.*;
import arc.util.*;
import mindustry.gen.*;

import java.util.*;

/**
 * Defines a unit of measurement for block stats.
 */
public class StatUnit{
    public static final StatUnit

    blocks = new StatUnit("blocks"),
    blocksSquared = new StatUnit("blocksSquared"),
    tilesSecond = new StatUnit("tilesSecond"),
    powerSecond = new StatUnit("powerSecond", "[accent]" + Iconc.power + "[]"),
    liquidSecond = new StatUnit("liquidSecond", "[sky]" + Iconc.liquid + "[]"),
    itemsSecond = new StatUnit("itemsSecond"),
    liquidUnits = new StatUnit("liquidUnits", "[sky]" + Iconc.liquid + "[]"),
    powerUnits = new StatUnit("powerUnits", "[accent]" + Iconc.power + "[]"),
    heatUnits = new StatUnit("heatUnits", "[red]" + Iconc.waves + "[]"),
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
    public @Nullable String icon;

    public StatUnit(String name, boolean space){
        this.name = name;
        this.space = space;
    }

    public StatUnit(String name){
        this(name, true);
    }

    public StatUnit(String name, String icon){
        this(name, true);
        this.icon = icon;
    }

    public String localized(){
        if(this == none) return "";
        return Core.bundle.get("unit." + name.toLowerCase(Locale.ROOT));
    }
}
