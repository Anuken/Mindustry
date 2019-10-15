package io.anuke.mindustry.world.blocks.storage;

import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

public abstract class StorageBlock extends Block{

    public StorageBlock(String name){
        super(name);
        hasItems = true;
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        StorageBlockEntity entity = tile.entity();
        return entity.linkedCore != null ? entity.linkedCore.block().acceptItem(item, entity.linkedCore, source) : tile.entity.items.get(item) < getMaximumAccepted(tile, item);
    }

    @Override
    public int getMaximumAccepted(Tile tile, Item item){
        return itemCapacity;
    }

    @Override
    public void drawSelect(Tile tile){
        StorageBlockEntity entity = tile.entity();
        if(entity.linkedCore != null){
            entity.linkedCore.block().drawSelect(entity.linkedCore);
        }
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    /**
     * Removes an item and returns it. If item is not null, it should return the item.
     * Returns null if no items are there.
     */
    public Item removeItem(Tile tile, Item item){
        TileEntity entity = tile.entity;

        if(item == null){
            return entity.items.take();
        }else{
            if(entity.items.has(item)){
                entity.items.remove(item, 1);
                return item;
            }

            return null;
        }
    }

    /**
     * Returns whether this storage block has the specified item.
     * If the item is null, it should return whether it has ANY items.
     */
    public boolean hasItem(Tile tile, Item item){
        TileEntity entity = tile.entity;
        if(item == null){
            return entity.items.total() > 0;
        }else{
            return entity.items.has(item);
        }
    }

    @Override
    public TileEntity newEntity(){
        return new StorageBlockEntity();
    }

    public class StorageBlockEntity extends TileEntity{
        protected @Nullable
        Tile linkedCore;
    }
}
