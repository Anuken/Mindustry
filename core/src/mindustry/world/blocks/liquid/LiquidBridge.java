package mindustry.world.blocks.liquid;

import arc.math.*;
import arc.util.*;
import mindustry.entities.AllDefs.*;
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
    public void updateTile(){
        time += cycleSpeed * Time.delta();
        time2 += (cycleSpeed - 1f) * Time.delta();

        Tile other = world.tile(link);
        if(!linkValid(tile, other)){
            tryDumpLiquid(tile, liquids.current());
        }else{
            ((ItemBridgeEntity)world.tile(link).entity).incoming.add(tile.pos());

            if(consValid()){
                float alpha = 0.04f;
                if(hasPower){
                    alpha *= efficiency(); // Exceed boot time unless power is at max.
                }
                uptime = Mathf.lerpDelta(uptime, 1f, alpha);
            }else{
                uptime = Mathf.lerpDelta(uptime, 0f, 0.02f);
            }

            if(uptime >= 0.5f){

                if(tryMoveLiquid(tile, other, false, liquids.current()) > 0.1f){
                    cycleSpeed = Mathf.lerpDelta(cycleSpeed, 4f, 0.05f);
                }else{
                    cycleSpeed = Mathf.lerpDelta(cycleSpeed, 1f, 0.01f);
                }
            }
        }
    }

    @Override
    public boolean acceptItem(Tile source, Item item){
        return false;
    }
}
