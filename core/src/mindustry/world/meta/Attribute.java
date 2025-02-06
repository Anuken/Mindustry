package mindustry.world.meta;

import arc.struct.*;
import mindustry.*;

public class Attribute{
    public static Attribute[] all = {};
    public static ObjectMap<String, Attribute> map = new ObjectMap<>();

    /** Heat content. Used for thermal generator yield. */
    public static final Attribute
    heat = add("heat"),
    /** Spore content. Used for cultivator yield. */
    spores = add("spores"),
    /** Water content. Used for water extractor yield. */
    water = add("water"),
    /** Oil content. Used for oil extractor yield. */
    oil = add("oil"),
    /** Light coverage. Negative values decrease solar panel efficiency. */
    light = add("light"),
    /** Used for sand extraction. */
    sand = add("sand"),
    /** Used for erekir vents only. */
    steam = add("steam");

    public final int id;
    public final String name;

    /** @return the environmental value for this attribute. */
    public float env(){
        if(Vars.state == null) return 0;
        return Vars.state.envAttrs.get(this);
    }

    Attribute(int id, String name){
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString(){
        return name;
    }

    /** Never returns null, may throw an exception if not found. */
    public static Attribute get(String name){
        return map.getThrow(name, () -> new IllegalArgumentException("Unknown Attribute type: " + name));
    }

    /** @return Whether an attribute exists. */
    public static boolean exists(String name){
        return map.containsKey(name);
    }

    /** Automatically registers this attribute for use. Do not call after mod init. */
    public static Attribute add(String name){
        Attribute a = new Attribute(all.length, name);
        Attribute[] prev = all;
        all = new Attribute[all.length + 1];
        System.arraycopy(prev, 0, all, 0, a.id);
        all[a.id] = a;
        map.put(name, a);
        return a;
    }
}
