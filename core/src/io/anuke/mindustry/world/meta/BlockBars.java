package io.anuke.mindustry.world.meta;

import io.anuke.arc.collection.OrderedMap;
import io.anuke.arc.function.Function;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.ui.Bar;

public class BlockBars{
    private OrderedMap<String, Function<TileEntity, Bar>> bars = new OrderedMap<>();

    public void add(String name,Function<TileEntity, Bar> sup){
        bars.put(name, sup);
    }

    public void remove(String name){
        bars.remove(name);
    }

    public Iterable<Function<TileEntity, Bar>> list(){
        return bars.values();
    }
}
