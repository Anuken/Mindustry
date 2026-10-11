package mindustry.world.meta;

import arc.struct.*;
import arc.util.*;

/** Environmental flags for different types of locations. */
public class Env{
    public static final Seq<Env> all = new Seq<>();
    private static final ObjectMap<String, Env> byName = new ObjectMap<>();

    //note: vanilla ids must match the old bit positions (do not reorder them)
    public static final Env
    //is on a planet
    terrestrial = new Env("terrestrial"),
    //is in space, no atmosphere
    space = new Env("space"),
    //is underwater, on a planet
    underwater = new Env("underwater"),
    //has a spores
    spores = new Env("spores"),
    //has a scorching env effect
    scorching = new Env("scorching"),
    //has oil reservoirs
    groundOil = new Env("groundOil"),
    //has water reservoirs
    groundWater = new Env("groundWater"),
    //has oxygen in the atmosphere
    oxygen = new Env("oxygen");

    public final int id;
    public final String name;

    public Env(String name){
        if(name == null || name.isEmpty()) throw new IllegalArgumentException("Env name cannot be null or empty.");
        if(name.equals("any")) throw new IllegalArgumentException("'any' is a reserved Env name.");
        if(byName.containsKey(name)) throw new IllegalArgumentException("Duplicate Env name: '" + name + "'");

        this.name = name;
        this.id = all.size;
        all.add(this);
        byName.put(name, this);
    }

    public static @Nullable Env get(String name){
        return byName.get(name);
    }

    @Override
    public String toString(){
        return name;
    }
}
