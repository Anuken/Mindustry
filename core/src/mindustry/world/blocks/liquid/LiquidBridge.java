package mindustry.world.blocks.liquid;

import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.meta.*;

public class LiquidBridge extends ItemBridge{

    public LiquidBridge(String name){
        super(name);
        hasItems = false;
        hasLiquids = true;
        outputsLiquid = true;
        canOverdrive = false;
        group = BlockGroup.liquids;
    }

    public class LiquidBridgeBuild extends ItemBridgeBuild{

        @Override
        public void updateTransport(Building other){
            if(warmup >= 0.5f){
                moved |= moveLiquid(other, liquids.current()) > 0.05f;
            }
        }

        @Override
        public void doDump(){
            dumpLiquid(liquids.current(), 1f);
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return false;
        }
    }
}
