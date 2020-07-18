package mindustry.world.meta;

import arc.struct.OrderedMap;
import arc.func.Func;
import mindustry.gen.*;
import mindustry.ui.Bar;

public class BlockBars{
    private OrderedMap<String, Func<Building, Bar>> bars = new OrderedMap<>();

    public <T extends Building> void add(String name, Func<T, Bar> sup){
        bars.put(name, (Func<Building, Bar>)sup);
    }

    public void remove(String name){
        if(!bars.containsKey(name))
            throw new RuntimeException("No bar with name '" + name + "' found; current bars: " + bars.keys().toSeq());
        bars.remove(name);
    }

    public Iterable<Func<Building, Bar>> list(){
        return bars.values();
    }
}
