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
        entityType = StorageBlockEntity::new;
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        StorageBlockEntity entity = tile.ent();
        return entity.linkedCore != null ? entity.linkedCore.block().acceptItem(item, entity.linkedCore, source) : tile.entity.getItems().get(item) < getMaximumAccepted(tile, item);
    }

    @Override
    public int getMaximumAccepted(Tile tile, Item item){
        return itemCapacity;
    }

    @Override
    public void drawSelect(Tile tile){
        StorageBlockEntity entity = tile.ent();
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
        Tilec entity = tile.entity;

        if(item == null){
            return entity.getItems().take();
        }else{
            if(entity.getItems().has(item)){
                entity.getItems().remove(item, 1);
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
        Tilec entity = tile.entity;
        if(item == null){
            return entity.getItems().total() > 0;
        }else{
            return entity.getItems().has(item);
        }
    }

    public class StorageBlockEntity extends Tilec{
        protected @Nullable
        Tile linkedCore;
    }
}
