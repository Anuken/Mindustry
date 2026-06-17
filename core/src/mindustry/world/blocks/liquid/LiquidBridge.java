package mindustry.world.blocks.liquid;

import arc.util.*;
import mindustry.gen.*;
import mindustry.world.blocks.*;
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
        envEnabled = Env.any;
    }

    public class LiquidBridgeBuild extends ItemBridgeBuild implements LiquidUpdater{

        @Override
        public void updateTransport(Building other){

        }

        @Override
        public void doDump(){

        }

        @Override
        public void updateLiquids(float delta){
            var link = lastValidLink;

            if(link == null){
                dumpLiquid(liquids.current(), 1f);
            }else if(warmup >= 0.25f){
                moved |= moveLiquid(link, liquids.current(), Time.delta) > 0.05f;
            }
        }
    }
}
