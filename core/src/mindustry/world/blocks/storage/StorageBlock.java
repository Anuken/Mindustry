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

    public class StorageBlockEntity extends Building{
        protected @Nullable Building linkedCore;

        @Override
        public boolean acceptItem(Building source, Item item){
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
