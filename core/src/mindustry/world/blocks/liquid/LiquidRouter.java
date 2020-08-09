package mindustry.world.blocks.liquid;

import mindustry.gen.*;
import mindustry.type.*;

public class LiquidRouter extends LiquidBlock{

    public LiquidRouter(String name){
        super(name);
    }

    public class LiquidRouterEntity extends LiquidBlockEntity{
        @Override
        public void updateTile(){
            if(liquids.total() > 0.01f){
                dumpLiquid(liquids.current());
            }
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid, float amount){
            return liquids.get(liquid) + amount < liquidCapacity && (liquids.current() == liquid || liquids.currentAmount() < 0.2f);
        }
    }
}
