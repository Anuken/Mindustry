package mindustry.plugin.spiderweb;

import arc.struct.*;
import arc.util.*;
import mindustry.world.*;

import static mindustry.Vars.spiderWeb;

public class Spiderling{
    public String uuid;

    public Array<String> names = new Array<>();
    public ObjectSet<Block> unlocked = new ObjectSet<>();

    public void save(){
        spiderWeb.save(this);
    }

    public void log(){
        Log.warn("names: " + names);
        Log.warn("unlocked: " + unlocked);
    }
}
