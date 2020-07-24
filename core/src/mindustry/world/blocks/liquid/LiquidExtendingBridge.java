package mindustry.world.blocks.liquid;

import arc.math.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.meta.*;

import static mindustry.Vars.world;

public class LiquidExtendingBridge extends ExtendingItemBridge{

    public LiquidExtendingBridge(String name){
        super(name);
        hasItems = false;
        hasLiquids = true;
        outputsLiquid = true;
        group = BlockGroup.liquids;
    }

    public class LiquidExtendingBridgeEntity extends ExtendingItemBridgeEntity{
        @Override
        public void updateTile(){
            time += cycleSpeed * delta();
            time2 += (cycleSpeed - 1f) * delta();

            checkIncoming();

            Building other = world.build(link);
            if(other == null || !linkValid(tile, other.tile())){
                dumpLiquid(liquids.current());
            }else{
                ((ItemBridgeEntity)other).incoming.add(tile.pos());

                if(consValid()){
                    uptime = Mathf.lerpDelta(uptime, 1f, 0.04f);
                }else{
                    uptime = Mathf.lerpDelta(uptime, 0f, 0.02f);
                }

                if(uptime >= 0.5f){
                    if(moveLiquid(other, liquids.current()) > 0.1f){
                        cycleSpeed = Mathf.lerpDelta(cycleSpeed, 4f, 0.05f);
                    }else{
                        cycleSpeed = Mathf.lerpDelta(cycleSpeed, 1f, 0.01f);
                    }
                }
            }
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return false;
        }
    }
}
