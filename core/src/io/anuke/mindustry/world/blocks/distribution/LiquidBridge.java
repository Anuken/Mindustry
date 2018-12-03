package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockGroup;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.world;

public class LiquidBridge extends ItemBridge{

    public LiquidBridge(String name){
        super(name);
        hasItems = false;
        hasLiquids = true;
        outputsLiquid = true;
        group = BlockGroup.liquids;
    }

    @Override
    public void update(Tile tile){
        ItemBridgeEntity entity = tile.entity();

        entity.time += entity.cycleSpeed * Timers.delta();
        entity.time2 += (entity.cycleSpeed - 1f) * Timers.delta();

        Tile other = world.tile(entity.link);
        if(!linkValid(tile, other) ){
            tryDumpLiquid(tile, entity.liquids.current());
        }else{
            if(entity.cons.valid()){
                float alpha = 0.04f;
                if(hasPower){
                    alpha *= entity.power.satisfaction; // Exceed boot time unless power is at max.
                }
                entity.uptime = Mathf.lerpDelta(entity.uptime, 1f, alpha);
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

    @Override
    public boolean canDumpLiquid(Tile tile, Tile to, Liquid liquid){
        ItemBridgeEntity entity = tile.entity();

        Tile other = world.tile(entity.link);
        if(!linkValid(tile, other)){
            return !(to.block() instanceof LiquidBridge);
        }

        int rel = tile.absoluteRelativeTo(other.x, other.y);
        int rel2 = tile.relativeTo(to.x, to.y);

        return rel != rel2;
    }
}
