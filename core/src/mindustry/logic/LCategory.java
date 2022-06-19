package mindustry.logic;

import arc.*;
import arc.graphics.*;
import arc.scene.style.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class LCategory implements Comparable<LCategory>{
    public static final Seq<LCategory> all = new Seq<>();

    public static final LCategory

    unknown = new LCategory("unknown", Pal.darkishGray),
    io = new LCategory("io", Pal.logicIo, Icon.logicSmall),
    block = new LCategory("block", Pal.logicBlocks, Icon.effectSmall),
    operation = new LCategory("operation", Pal.logicOperations, Icon.settingsSmall),
    control = new LCategory("control", Pal.logicControl, Icon.rotateSmall),
    unit = new LCategory("unit", Pal.logicUnits, Icon.unitsSmall),
    world = new LCategory("world", Pal.logicWorld, Icon.terminalSmall);

    public final String name;
    public final int id;
    public final Color color;

    @Nullable
    public final Drawable icon;

    public LCategory(String name, Color color){
        this(name, color,null);
    }

    public LCategory(String name, Color color, Drawable icon){
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
    public int compareTo(LCategory o){
        return id - o.id;
    }
}
