package io.anuke.mindustry.world.blocks.sandbox;

import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

public class ItemVoid extends Block{

    public ItemVoid(String name){
        super(name);
        update = solid = true;
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return true;
    }
}
