package io.anuke.mindustry.world.blocks.types.storage;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

public abstract class StorageBlock extends Block {

    public StorageBlock(String name){
        super(name);
    }

    /**Removes any one item and returns it. Returns null if no items are there.*/
    public Item removeItem(Tile tile){
        TileEntity entity = tile.entity;
        for(int i = 0; i < entity.items.length; i ++){
            if(entity.items[i] > 0){
                entity.items[i] --;
                return Item.getByID(i);
            }
        }
        return null;
    }
}
