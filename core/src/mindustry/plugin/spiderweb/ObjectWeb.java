package mindustry.plugin.spiderweb;

import arc.func.*;
import arc.struct.*;

import static mindustry.Vars.spiderweb;

public class ObjectWeb<T> extends ObjectSet<T>{

    Cons<SpiderWeb>    loader = (web) -> {};
    Cons2<SpiderWeb, T> adder = (web, T) -> {};

    private boolean ready = false;

    public void ready(){
        ready = true;
    }

    @Override
    public boolean add(T key){
        if(ready) adder.get(spiderweb, key);
        return super.add(key);
    }
}
