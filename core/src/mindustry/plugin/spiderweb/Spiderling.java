package mindustry.plugin.spiderweb;

import arc.struct.*;
import arc.util.*;

import static mindustry.Vars.spiderWeb;

public class Spiderling{
    public String uuid;

    public Array<String> names;

    public void save(){
        spiderWeb.saveNames(this);
    }

    public void log(){
        Log.warn("names: " + names);
    }
}
