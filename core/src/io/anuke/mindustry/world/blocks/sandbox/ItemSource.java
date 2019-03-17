package io.anuke.mindustry.world.blocks.sandbox;

import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.distribution.Sorter;

public class ItemSource extends Sorter{

    public ItemSource(String name){
        super(name);
        hasItems = true;
        update = true;
        solid = true;
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.remove("items");
    }

    @Override
    public boolean outputsItems(){
        return true;
    }

    @Override
    public void update(Tile tile){
        SorterEntity entity = tile.entity();
        if(entity.sortItem == null) return;

        entity.items.set(entity.sortItem, 1);
        tryDump(tile, entity.sortItem);
        entity.items.set(entity.sortItem, 0);
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return false;
    }
}
