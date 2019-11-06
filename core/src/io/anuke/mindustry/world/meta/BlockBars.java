package io.anuke.mindustry.world.meta;

import io.anuke.arc.collection.OrderedMap;
import io.anuke.arc.func.Func;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.ui.Bar;

public class BlockBars{
    private OrderedMap<String, Func<TileEntity, Bar>> bars = new OrderedMap<>();

    public void add(String name, Func<TileEntity, Bar> sup){
        bars.put(name, sup);
    }

    public void remove(String name){
        if(!bars.containsKey(name))
            throw new RuntimeException("No bar with name '" + name + "' found; current bars: " + bars.keys().toArray());
        bars.remove(name);
    }

    public Iterable<Func<TileEntity, Bar>> list(){
        return bars.values();
    }
}
