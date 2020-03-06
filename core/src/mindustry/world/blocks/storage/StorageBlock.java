package mindustry.world.blocks.storage;

import arc.util.ArcAnnotate.*;
import mindustry.gen.*;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.Tile;

public abstract class StorageBlock extends Block{

    public StorageBlock(String name){
        super(name);
        hasItems = true;
    }

    @Override
    public boolean acceptItem(Tilec source, Item item){
        return linkedCore != null ? linkedCore.acceptItem(linkedCore, source, item) : tile.items.get(item) < getMaximumAccepted(tile, item);
    }

    @Override
    public int getMaximumAccepted(Item item){
        return itemCapacity;
    }

    @Override
    public void drawSelect(){
        if(linkedCore != null){
            linkedCore.block().drawSelect(linkedCore);
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
    public Item removeItem(Item item){
        Tilec entity = tile.entity;

        if(item == null){
            return items.take();
        }else{
            if(items.has(item)){
                items.remove(item, 1);
                return item;
            }

            return null;
        }
    }

    /**
     * Returns whether this storage block has the specified item.
     * If the item is null, it should return whether it has ANY items.
     */
    public boolean hasItem(Item item){
        Tilec entity = tile.entity;
        if(item == null){
            return items.total() > 0;
        }else{
            return items.has(item);
        }
    }

    public class StorageBlockEntity extends TileEntity{
        protected @Nullable
        Tile linkedCore;
    }
}
