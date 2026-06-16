package mindustry.world.blocks.liquid;

import mindustry.gen.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class LiquidBridge extends ItemBridge{

    public LiquidBridge(String name){
        super(name);
        hasItems = false;
        hasLiquids = true;
        outputsLiquid = true;
        canOverdrive = false;
        group = BlockGroup.liquids;
        envEnabled = Env.any;
    }

    public class LiquidBridgeBuild extends ItemBridgeBuild{

        @Override
        public void addToList(){
            state.buildings.liquidBridges.add(this);
        }

        @Override
        public void removeFromList(){
            state.buildings.liquidBridges.remove(this);
        }

        @Override
        public void updateTransport(Building other){
            if(warmup >= 0.25f){
                moved |= moveLiquid(other, liquids.current()) > 0.05f;
            }
        }

        @Override
        public void doDump(){
            dumpLiquid(liquids.current(), 1f);
        }
    }
}
