package mindustry.world.blocks.storage;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock.*;
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

    public static void incinerateEffect(Building self, Building source){
        if(Mathf.chance(0.1)){
            Tile edge = Edges.getFacingEdge(source, self);
            Tile edge2 = Edges.getFacingEdge(self, source);
            if(edge != null && edge2 != null){
                Fx.fuelburn.at((edge.worldx() + edge2.worldx())/2f, (edge.worldy() + edge2.worldy())/2f);
            }
        }
    }

    public class StorageBuild extends Building{
        protected @Nullable Building linkedCore;

        @Override
        public boolean acceptItem(Building source, Item item){
            return linkedCore != null ? linkedCore.acceptItem(source, item) : items.get(item) < getMaximumAccepted(item);
        }

        @Override
        public void handleItem(Building source, Item item){
            if(linkedCore != null){
                incinerateEffect(this, source);
                ((CoreBuild)linkedCore).noEffect = true;
                linkedCore.handleItem(source, item);
            }else{
                super.handleItem(source, item);
            }
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
                    items.add(other.items);
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
