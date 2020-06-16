package mindustry.world.meta;

import arc.struct.OrderedMap;
import arc.func.Func;
import mindustry.gen.*;
import mindustry.ui.Bar;

public class BlockBars{
    private OrderedMap<String, Func<Tilec, Bar>> bars = new OrderedMap<>();

    public void add(String name, Func<Tilec, Bar> sup){
        bars.put(name, sup);
    }

    public void remove(String name){
        if(!bars.containsKey(name))
            throw new RuntimeException("No bar with name '" + name + "' found; current bars: " + bars.keys().toSeq());
        bars.remove(name);
    }

    public Iterable<Func<Tilec, Bar>> list(){
        return bars.values();
    }
}
