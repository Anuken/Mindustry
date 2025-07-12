package mindustry.ai.types;

import mindustry.ai.*;
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
    public void stanceChanged(){
        if(targetItem != null && unit.controller() instanceof CommandAI ai && !ai.hasStance(UnitStance.mineAuto) && !ai.hasStance(ItemUnitStance.getByItem(targetItem))){
            mining = false;
            targetItem = null;
        }
    }

    @Override
    public void updateMovement(){
        Building core = unit.closestCore();

        if(!unit.canMine() || core == null) return;

        if(!unit.validMine(unit.mineTile)){
            unit.mineTile(null);
        }

        CommandAI ai = unit.controller() instanceof CommandAI a ? a : null;

        if(mining){
            if(timer.get(timerTarget2, 60 * 4) || targetItem == null){
                if(ai != null && !ai.hasStance(UnitStance.mineAuto)){
                    targetItem = content.items().min(i -> indexer.hasOre(i) && unit.canMine(i) && ai.hasStance(ItemUnitStance.getByItem(i)), i -> core.items.get(i));
                }else{
                    targetItem = unit.type.mineItems.min(i -> indexer.hasOre(i) && unit.canMine(i), i -> core.items.get(i));
                }
            }

            //core full of the target item, do nothing
            if(targetItem != null && core.acceptStack(targetItem, 1, unit) == 0){
                unit.clearItem();
                unit.mineTile = null;
                return;
            }

            //if inventory is full, drop it off.
            if(unit.stack.amount >= unit.type.itemCapacity || (targetItem != null && !unit.acceptsItem(targetItem))){
                mining = false;
            }else{
                if(timer.get(timerTarget3, 60) && targetItem != null){
                    ore = null;
                    if(unit.type.mineFloor) ore = indexer.findClosestOre(core.x, core.y, targetItem);
                    if(ore == null && unit.type.mineWalls) ore = indexer.findClosestWallOre(core.x, core.y, targetItem);
                }

                if(ore != null){
                    moveTo(ore, unit.type.mineRange / 2f, 20f);

                    if(unit.within(ore, unit.type.mineRange) && unit.validMine(ore)){
                        unit.mineTile = ore;
                    }
                }
            }
        }else{
            unit.mineTile = null;

            if(unit.stack.amount == 0){
                mining = true;
                return;
            }

            if(unit.within(core, unit.type.range)){
                if(core.acceptStack(unit.stack.item, unit.stack.amount, unit) > 0){
                    Call.transferItemTo(unit, unit.stack.item, unit.stack.amount, unit.x, unit.y, core);
                }

                unit.clearItem();
                mining = true;
            }

            circle(core, unit.type.range / 1.8f);
        }

        if(!unit.type.flying){
            unit.updateBoosting(unit.type.boostWhenMining || unit.floorOn().isDuct || unit.floorOn().damageTaken > 0f || unit.floorOn().isDeep());
        }
    }
}