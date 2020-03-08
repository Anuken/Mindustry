package mindustry.plugin.spiderweb;

import arc.struct.*;
import arc.util.*;
import mindustry.world.*;

import static mindustry.Vars.spiderweb;

public class Spiderling{
    public String uuid;

    public ObjectSet<String> names = new ObjectSet<>();
    public ObjectSet<Block> unlockedBlocks = new ObjectSet<>();

    public void load(){
        spiderweb.loadNames(this);
        spiderweb.loadUnlockedBlocks(this);
    }

    public void save(){
        spiderweb.saveNames(this);
        spiderweb.saveUnlockedBlocks(this);
    }

    public void log(){
        Log.warn("names: " + names);
        Log.warn("unlocked: " + unlockedBlocks);
    }
}
