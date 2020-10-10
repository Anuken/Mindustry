package mindustry.world.blocks.storage;

import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class StorageBlock extends Block{
    public StorageBlock(String name){
        super(name);
        hasItems = true;
        solid = true;
        update = false;
        destructible = true;
        group = BlockGroup.storage;
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    public class StorageBuild extends Building{
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

        @Override
        public void overwrote(Seq<Building> previous){
            for(Building other : previous){
                if(other.items != null){
                    items.addAll(other.items);
                }
            }

            //ensure item counts are not too high
            items.each((i, a) -> items.set(i, Math.min(a, itemCapacity)));
        }

        @Override
        public boolean canPickup(){
            return linkedCore == null;
        }
    }
}
