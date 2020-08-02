package mindustry.world.blocks.storage;

import arc.*;
import arc.util.ArcAnnotate.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
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

    @Override
    public void setStats(){
        super.setStats();

        bars.add("capacity", (StorageBlockEntity e) ->
            new Bar(
                () -> Core.bundle.format("bar.capacity", UI.formatAmount(itemCapacity)),
                () -> Pal.items,
            () -> e.items.total() / (float)itemCapacity
        ));
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
