package mindustry.logic;

import arc.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;

public class LCategory implements Comparable<LCategory>{
    public static final Seq<LCategory> all = new Seq<>();

    public static final LCategory

    unknown = new LCategory("unknown"),
    io = new LCategory("io", Icon.logicSmall),
    block = new LCategory("block", Icon.effectSmall),
    operation = new LCategory("operation", Icon.settingsSmall),
    control = new LCategory("control", Icon.rotateSmall),
    unit = new LCategory("unit", Icon.unitsSmall),
    world = new LCategory("world", Icon.terminalSmall);

    public final String name;
    public final int id;

    @Nullable
    public final Drawable icon;

    public LCategory(String name){
        this(name, null);
    }

    public LCategory(String name, Drawable icon){
        this.icon = icon;
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
