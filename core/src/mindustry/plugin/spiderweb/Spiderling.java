package mindustry.plugin.spiderweb;

import arc.struct.*;
import arc.util.*;
import mindustry.world.*;

import static mindustry.Vars.spiderWeb;

public class Spiderling{
    public String uuid;

    public ObjectSet<String> names = new ObjectSet<>();
    public ObjectSet<Block> unlockedBlocks = new ObjectSet<>();

    public void load(){
        spiderWeb.loadNames(this);
        spiderWeb.loadUnlockedBlocks(this);
    }

    public void save(){
        spiderWeb.saveNames(this);
        spiderWeb.saveUnlockedBlocks(this);
    }

    public void log(){
        Log.warn("names: " + names);
        Log.warn("unlocked: " + unlockedBlocks);
    }
}
