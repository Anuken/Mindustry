package mindustry.ai.types;

import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class MinerAI extends AIController{
    public boolean mining = true;
    public Item targetItem;
    public Tile ore;

    @Override
    public void updateMovement(){
        Building core = unit.closestCore();

        if(!(unit.canMine()) || core == null) return;

        if(unit.mineTile != null && !unit.mineTile.within(unit, unit.type.mineRange)){
            unit.mineTile(null);
        }

        if(mining){
            if(timer.get(timerTarget2, 60 * 4) || targetItem == null){
                targetItem = unit.type.mineItems.min(i -> indexer.hasOre(i) && unit.canMine(i), i -> core.items.get(i));
            }

            //if inventory is full, drop it off.
            if(unit.stack.amount >= unit.type.itemCapacity || (targetItem != null && !unit.acceptsItem(targetItem))){
                mining = false;
            }else{
                if(timer.get(timerTarget3, 60) && targetItem != null){
                    ore = indexer.findClosestOre(unit, targetItem);
                }

                if(ore != null){
                    moveTo(ore, unit.type.mineRange / 2f, 20f);

                    if(ore.block() == Blocks.air && unit.within(ore, unit.type.mineRange)){
                        unit.mineTile = ore;
                    }

                    if(ore.block() != Blocks.air){
                        mining = false;
                    }
                }
            }
        }else{
            unit.mineTile = null;

            if(unit.stack.amount == 0){
                mining = true;
                return;
            }

            if(core.health < core.maxHealth && unit.within(core, unit.type.range)){
                Call.effect(Fx.greenCloud, core.x, core.y, core.rotation, core.team.color);
                Call.effect(Fx.greenCloud, unit.x, unit.y, unit.rotation, unit.team.color);
                core.heal(unit.stack.item.hardness * unit.stack.amount * core.block.size);

                unit.clearItem();
                mining = true;
                return;
            }

            if(core.health < core.maxHealth){
                moveTo(core, unit.type.range / 1.5f);
            }else{
                circle(core, unit.type.range * 3f);
            }
        }
    }
}