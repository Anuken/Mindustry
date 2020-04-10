package mindustry.world.blocks.liquid;

import arc.math.*;
import arc.util.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.meta.*;

import static mindustry.Vars.world;

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
        ItemBridgeEntity entity = tile.ent();

        entity.time += entity.cycleSpeed * Time.delta();
        entity.time2 += (entity.cycleSpeed - 1f) * Time.delta();

        Tile other = world.tile(entity.link);
        if(!linkValid(tile, other)){
            tryDumpLiquid(tile, entity.liquids.current());
        }else{
            ((ItemBridgeEntity)world.tile(entity.link).entity).incoming.add(tile.pos());

            if(entity.cons.valid()){
                float alpha = 0.04f;
                if(hasPower){
                    alpha *= entity.efficiency(); // Exceed boot time unless power is at max.
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
}
