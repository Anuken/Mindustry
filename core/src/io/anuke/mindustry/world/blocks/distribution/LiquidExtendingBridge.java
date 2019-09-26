package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.arc.math.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.meta.*;

import static io.anuke.mindustry.Vars.world;

public class LiquidExtendingBridge extends ExtendingItemBridge{

    public LiquidExtendingBridge(String name){
        super(name);
        hasItems = false;
        hasLiquids = true;
        outputsLiquid = true;
        group = BlockGroup.liquids;
    }

    @Override
    public void update(Tile tile){
        ItemBridgeEntity entity = tile.entity();

        entity.time += entity.cycleSpeed * Time.delta();
        entity.time2 += (entity.cycleSpeed - 1f) * Time.delta();

        Tile other = world.tile(entity.link);
        if(!linkValid(tile, other)){
            tryDumpLiquid(tile, entity.liquids.current());
        }else{
            if(entity.cons.valid()){
                entity.uptime = Mathf.lerpDelta(entity.uptime, 1f, 0.04f);
            }else{
                entity.uptime = Mathf.lerpDelta(entity.uptime, 0f, 0.02f);
            }

            if(entity.uptime >= 0.5f){

                if(tryMoveLiquid(tile, other, false, entity.liquids.current()) > 0.1f){
                    entity.cycleSpeed = Mathf.lerpDelta(entity.cycleSpeed, 4f, 0.05f);
                }else{
                    entity.cycleSpeed = Mathf.lerpDelta(entity.cycleSpeed, 1f, 0.01f);
                }
            }
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return false;
    }
}
