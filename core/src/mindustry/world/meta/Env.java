package mindustry.world.meta;

import arc.struct.*;
import arc.util.*;

/** Environmental flags for different types of locations. */
public class Env{
    public static final int
    //is on a planet
    terrestrial,
    //is in space, no atmosphere
    space,
    //is underwater, on a planet
    underwater,
    //has a spores
    spores,
    //has a scorching env effect
    scorching,
    //has oil reservoirs
    groundOil,
    //has water reservoirs
    groundWater,
    //has oxygen in the atmosphere
    oxygen,
    //all attributes combined, only used for bitmasking purposes
    any = 0xffffffff,
    //no attributes (0)
    none = 0;

    //do NOT modify directly!
    public static final ObjectIntMap<String> nameToId;
    public static final IntMap<String> idToName;

    static{
        //last time i didn't use a static initializer i got a null pointer.
        //i can probably just move the fields around, but i don't trust java enough for that
        nameToId = new ObjectIntMap<>();
        idToName = new IntMap<>();

        terrestrial = add("terrestrial");
        space = add("space");
        underwater = add("underwater");
        spores = add("spores");
        scorching = add("scorching");
        groundOil = add("groundOil");
        groundWater = add("groundWater");
        oxygen = add("oxygen");
    }

    public static int add(String key){
        if(nameToId.containsKey(key)) throw new IllegalArgumentException("'" + key + "' env already exists.");
        if(nameToId.size >= 32) throw new IllegalStateException("Max env count 32 exceeded.");

        int id = 1 << nameToId.size;
        nameToId.put(key, id);
        idToName.put(id, key);
        return id;
    }

    public static int remap(int mask, IntMap<String> idToName){
        int out = 0;
        for(int i = 0; i < 32; i++){
            int key = 1 << i;
            if((mask & key) == key){
                String name = idToName.get(key);
                if(name == null){
                    //if it's unmapped it's probably just some mods using constant value
                    out |= key;
                    continue;
                }

                int id = nameToId.get(name, -1);
                if(id == -1){
                    Log.warn("Ignoring '@' env key.", name);
                }else{
                    out |= id;
                }
            }
        }

        return out;
    }
}
