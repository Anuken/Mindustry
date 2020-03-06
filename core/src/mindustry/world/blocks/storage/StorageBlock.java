package mindustry.world.blocks.storage;

import arc.util.ArcAnnotate.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

public abstract class StorageBlock extends Block{

    public StorageBlock(String name){
        super(name);
        hasItems = true;
        solid = true;
        update = false;
        destructible = true;
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    public class StorageBlockEntity extends TileEntity{
        protected @Nullable Tilec linkedCore;

        /**
         * Removes an item and returns it. If item is not null, it should return the item.
         * Returns null if no items are there.
         */
        @Nullable
        public Item removeItem(@Nullable Item item){
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
        public boolean hasItem(@Nullable Item item){
            if(item == null){
                return items.total() > 0;
            }else{
                return items.has(item);
            }
        }

        @Override
        public boolean acceptItem(Tilec source, Item item){
            return linkedCore != null ? linkedCore.acceptItem(source, item) : items.get(item) < getMaximumAccepted(item);
        }

        @Override
        public int getMaximumAccepted(Item item){
            return itemCapacity;
        }

        @Override
        public void drawSelect(){
            if(linkedCore != null){
                linkedCore.drawSelect();
            }
        }
    }
}
