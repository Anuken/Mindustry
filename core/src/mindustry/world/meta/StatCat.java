package mindustry.world.meta;

import arc.*;
import arc.struct.*;

/** A specific category for a stat. */
public class StatCat implements Comparable<StatCat>{
    public static final Seq<StatCat> all = new Seq<>();

    public static final StatCat

    general = new StatCat("general"),
    power = new StatCat("power"),
    liquids = new StatCat("liquids"),
    items = new StatCat("items"),
    crafting = new StatCat("crafting"),
    function = new StatCat("function"),
    optional = new StatCat("optional");

    public final String name;
    public final int id;

    public StatCat(String name){
        this.name = name;
        id = all.size;
        all.add(this);
    }

    public String localized(){
        return Core.bundle.get("category." + name);
    }

    @Override
    public String toString(){
        return name;
    }

    @Override
    public int compareTo(StatCat o){
        return id - o.id;
    }
}
