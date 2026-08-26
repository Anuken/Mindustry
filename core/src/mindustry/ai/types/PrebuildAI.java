package mindustry.ai.types;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;

//don't use this yet, it's not functional!
public class PrebuildAI extends AIController{
    static float[] priorities = new float[Category.all.length];

    static Seq<BlockPlan> tmpCopy = new Seq<>();

    @Nullable BlockPlan lastPlan;
    @Nullable Block collectBlock;

    boolean collectingItems;
    boolean mining;
    @Nullable Item lastTargetItem;
    @Nullable Tile ore;

    static{
        priorities[Category.production.ordinal()] = 11f;
        priorities[Category.distribution.ordinal()] = 10f;
        priorities[Category.liquid.ordinal()] = 9f;
        priorities[Category.crafting.ordinal()] = 8f;

    }


    //TODO move this function?
    public static void sortPlans(Queue<BlockPlan> plans){
        var copy = Seq.with(plans);

        copy.sort(Structs.comps(
        Structs.comparingFloat(plan -> priorities[plan.block.category.ordinal()]),
        Structs.comparingFloat(plan -> plan.block.buildTime)
        ));

        plans.clear();

        for(var plan : copy){
            plans.addFirst(plan);
        }
    }

    private boolean canBuild(CoreBuild core, Block block){
        return state.rules.infiniteResources || unit.team.rules().infiniteResources || core.items.has(block.requirements, state.rules.buildCostMultiplier);
    }

    private @Nullable BlockPlan findNextPlan(){
        var data = unit.team.data();
        var core = unit.core();
        if(data.buildingTree == null || core == null) return null;
        var plans = data.plans;

        //TODO super slow just inline the search
        tmpCopy.clear();
        tmpCopy.addAll(plans);

        //TODO: this search is really slow
        var min = tmpCopy.min(plan ->
        (canBuild(core, plan.block) || !Structs.contains(plan.block.requirements, it -> !indexer.hasOre(it.item))) &&
        (plan.block.category == Category.production || data.buildingTree.any(plan.x * tilesize + plan.block.offset - (plan.block.size * tilesize + 1f)/2f, plan.y * tilesize + plan.block.offset- (plan.block.size * tilesize + 1f)/2f, plan.block.size * tilesize + 1f, plan.block.size * tilesize + 1f)),
        plan -> unit.dst(plan.x * tilesize, plan.y * tilesize) - priorities[plan.block.category.ordinal()] * 200f);

        if(min != null){
            return min;
        }

        return plans.first();
        /*
        for(int i = searchIndex; i < Math.min(maxSearches, size); i++){
            searchIndex ++;
            int index = (i + startIndex) % size + head;
            if(index >= values.length){
                index -= values.length;
            }

            var plan = plans.get((i + startIndex) % plans.size);
            if(plan != null){
                var block = plan.block;

                if(data.buildingTree != null && data.buildingTree.any(plan.x * tilesize + block.offset, plan.y * tilesize + block.offset, block.size * tilesize + 1f, block.size * tilesize + 1f)){
                    return plan;
                }
            }
        }*/
        //return null;
    }

    @Override
    public void updateMovement(){

        if(target != null && shouldShoot()){
            unit.lookAt(target);
        }else if(!unit.type.flying){
            unit.lookAt(unit.prefRotation());
        }

        unit.updateBuilding = !collectingItems;

        boolean moving = false;

        if(collectingItems){
            doMining();
        }else if(unit.buildPlan() != null){
            //approach plan if building
            BuildPlan req = unit.buildPlan();

            boolean valid =
            !(lastPlan != null && lastPlan.removed) &&
            ((req.tile() != null && req.tile().build instanceof ConstructBuild cons && cons.current == req.block) ||
            (req.breaking ?
            Build.validBreak(unit.team(), req.x, req.y) :
            Build.validPlace(req.block, unit.team(), req.x, req.y, req.rotation)));

            if(valid){
                float range = Math.min(unit.type.buildRange - 20f, 100f);
                //move toward the plan
                moveTo(req.tile(), range - 10f, 20f);
                moving = !unit.within(req.tile(), range);
            }else{
                //discard invalid plan
                unit.plans.removeFirst();
                lastPlan = null;
            }
        }else{

            //find new plan
            if(!unit.team.data().plans.isEmpty() && timer.get(timerTarget3, 2f)){
                //Queue<BlockPlan> blocks = unit.team.data().plans;
                BlockPlan plan = findNextPlan();

                //check if it's already been placed
                //if(world.tile(block.x, block.y) != null && world.tile(block.x, block.y).block() == block.block){
                //    blocks.removeFirst();
                //}else
                if(plan != null && Build.validPlace(plan.block, unit.team(), plan.x, plan.y, plan.rotation)){ //it's valid
                    if(!canBuild(unit.core(), plan.block)){
                        collectingItems = true;
                        collectBlock = plan.block;
                        lastTargetItem = null;
                        ore = null;
                        timer.reset(timerTarget, 0f);
                    }
                    lastPlan = plan;
                    unit.addBuild(new BuildPlan(plan.x, plan.y, plan.rotation, plan.block, plan.config));


                    //shift build plan to tail so next unit builds something else
                    //blocks.addLast(blocks.removeFirst());
                }//else{
                    //shift head of queue to tail, try something else next time
                 //   blocks.addLast(blocks.removeFirst());
                //}
            }
        }

        if(!unit.type.flying){
            unit.updateBoosting(unit.type.boostWhenBuilding || moving || unit.floorOn().isDuct || unit.floorOn().damageTaken > 0f || unit.floorOn().isDeep());
        }
    }

    void doMining(){
        var core = unit.closestCore();

        if(!unit.canMine() || core == null || collectBlock == null) return;

        if(!unit.validMine(unit.mineTile)){
            unit.mineTile(null);
        }

        if(mining){
            var targetStack = Structs.find(collectBlock.requirements, i -> !core.items.has(i.item, Mathf.ceil(state.rules.buildCostMultiplier * i.amount)));
            Item targetItem = targetStack == null ? null : targetStack.item;

            if(targetItem != null){
                lastTargetItem = targetItem;
            }else{
                targetItem = lastTargetItem;
                //hacky way to check if the unit just deposited something
                if(!unit.hasItem() && canBuild(core, collectBlock)){
                    collectingItems = false;
                    return;
                }
            }

            //core full of the target item, do nothing
            if(targetItem != null && core.acceptStack(targetItem, 1, unit) == 0){
                unit.clearItem();
                unit.mineTile = null;
                return;
            }

            //if inventory is full, drop it off.
            if(targetItem == null || unit.stack.amount >= unit.type.itemCapacity || (targetItem != null && !unit.acceptsItem(targetItem))){
                mining = false;
            }else{
                if(timer.get(timerTarget3, 60) && targetItem != null){
                    ore = null;
                    if(unit.type.mineFloor) ore = indexer.findClosestOre(unit, targetItem);
                    if(ore == null && unit.type.mineWalls) ore = indexer.findClosestWallOre(unit, targetItem);
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

                if(canBuild(core, collectBlock)){
                    collectingItems = false;
                }
                return;
            }

            if(unit.within(core, unit.type.range)){
                if(core.acceptStack(unit.stack.item, unit.stack.amount, unit) > 0){
                    Call.transferItemTo(unit, unit.stack.item, unit.stack.amount, unit.x, unit.y, core);
                }

                unit.clearItem();
                mining = true;

                if(canBuild(core, collectBlock)){
                    collectingItems = false;
                }
            }

            circle(core, unit.type.range / 1.8f);
        }
    }
}
