package mindustry.ai.types;

import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class MinerAI extends AIController{
    //miners are limited to copper and lead until further notice
    Seq<Item> targets = Seq.with(Items.copper, Items.lead);

    boolean mining = true;
    Item targetItem;
    Tile ore;

    @Override
    protected void updateMovement(){
        if(unit.moving()){
            unit.lookAt(unit.vel.angle());
        }

        if(unit.isFlying()){
            unit.wobble();
        }

        Building core = unit.closestCore();

        if(!(unit instanceof Minerc) || core == null) return;

        Minerc miner = (Minerc)unit;

        if(miner.mineTile() != null && !miner.mineTile().within(unit, unit.type().range)){
            miner.mineTile(null);
        }

        if(mining){
            targetItem = Structs.findMin(targets, indexer::hasOre, Structs.comparingInt(i -> -core.items.get(i)));

            //core full of the target item, do nothing
            if(targetItem != null && core.acceptStack(targetItem, 1, unit) == 0){
                unit.clearItem();
                return;
            }

            //if inventory is full, drop it off.
            if(unit.stack.amount >= unit.type().itemCapacity || (targetItem != null && !unit.acceptsItem(targetItem))){
                mining = false;
            }else{
                if(retarget() && targetItem != null){
                    ore = indexer.findClosestOre(unit.x, unit.y, targetItem);
                }

                if(ore != null){
                    moveTo(ore, unit.type().range / 1.5f);

                    if(unit.within(ore, unit.type().range)){
                        miner.mineTile(ore);
                    }

                    if(ore.block() != Blocks.air){
                        mining = false;
                    }
                }
            }
        }else{
            if(unit.stack.amount == 0){
                mining = true;
                return;
            }

            if(unit.within(core, unit.type().range)){
                if(core.acceptStack(unit.stack.item, unit.stack.amount, unit) > 0){
                    Call.transferItemTo(unit.stack.item, unit.stack.amount, unit.x, unit.y, core);
                }

                unit.clearItem();
                mining = true;
            }

            circle(core, unit.type().range / 1.8f);
        }
    }

    @Override
    protected void updateTargeting(){
    }

}
