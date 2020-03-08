package mindustry.plugin.spiderweb;

import arc.func.*;
import arc.struct.*;

import static mindustry.Vars.spiderweb;

public class ObjectWeb<T> extends ObjectSet<T>{

    Cons2<SpiderWeb, T> adder = (web, T) -> {};

    @Override
    public boolean add(T key){
        adder.get(spiderweb, key);
        return super.add(key);
    }
}
