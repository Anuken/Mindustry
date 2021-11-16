package mindustry.world.meta;

import arc.func.*;
import arc.struct.*;
import mindustry.gen.*;
import mindustry.ui.*;

public class BlockBars{
    private OrderedMap<String, Func<Building, Bar>> bars = new OrderedMap<>();

    public <T extends Building> void add(String name, Func<T, Bar> sup){
        bars.put(name, (Func<Building, Bar>)sup);
    }

    public void remove(String name){
        bars.remove(name);
    }

    public Iterable<Func<Building, Bar>> list(){
        return bars.values();
    }
}
