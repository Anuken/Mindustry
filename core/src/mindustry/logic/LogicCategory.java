package mindustry.logic;

import arc.*;
import arc.graphics.*;
import arc.scene.style.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class LogicCategory implements Comparable<LogicCategory>{
    public static final Seq<LogicCategory> all = new Seq<>();

    public static final LogicCategory

    unknown = new LogicCategory("unknown", Pal.darkishGray),
    io = new LogicCategory("io", Pal.logicIo, Icon.logicSmall),
    block = new LogicCategory("block", Pal.logicBlocks, Icon.effectSmall),
    operation = new LogicCategory("operation", Pal.logicOperations, Icon.settingsSmall),
    control = new LogicCategory("control", Pal.logicControl, Icon.rotateSmall),
    unit = new LogicCategory("unit", Pal.logicUnits, Icon.unitsSmall),
    world = new LogicCategory("world", Pal.logicWorld, Icon.terrainSmall);

    public final String name;
    public final int id;
    public final Color color;

    @Nullable
    public final Drawable icon;

    public LogicCategory(String name, Color color){
        this(name, color,null);
    }

    public LogicCategory(String name, Color color, Drawable icon){
        this.icon = icon;
        this.color = color;
        this.name = name;
        id = all.size;
        all.add(this);
    }

    public String localized(){
        return Core.bundle.get("lcategory." + name);
    }

    public String description(){
        return Core.bundle.get("lcategory." + name + ".description");
    }

    @Override
    public int compareTo(LogicCategory o){
        return id - o.id;
    }
}
