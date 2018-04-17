package io.anuke.mindustry.world.blocks.types.storage;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

public abstract class StorageBlock extends Block {

    public StorageBlock(String name){
        super(name);
    }

    /**Removes an item and returns it. If item is not null, it should return the item.
     * Returns null if no items are there.*/
    public Item removeItem(Tile tile, Item item){
        TileEntity entity = tile.entity;
        for(int i = 0; i < entity.items.items.length; i ++){
            if(entity.items.items[i] > 0 && (item == null || i == item.id)){
                entity.items.items[i] --;
                return Item.getByID(i);
            }
        }
        return null;
    }

    /**Returns whether this storage block has the specified item.
     * If the item is null, it should return whether it has ANY items.*/
    public boolean hasItem(Tile tile, Item item){
        TileEntity entity = tile.entity;
        for(int i = 0; i < entity.items.items.length; i ++){
            if(entity.items.items[i] > 0 && (item == null || i == item.id)){
                return true;
            }
        }
        return false;
    }
}
