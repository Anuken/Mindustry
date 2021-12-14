package mindustry.ai.types;

import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.units.UnitCargoUnloadPoint.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class CargoAI extends AIController{
    static Seq<Item> orderedItems = new Seq<>();
    static Seq<UnitCargoUnloadPointBuild> targets = new Seq<>();

    public static float emptyWaitTime = 60f * 2f, dropSpacing = 60f * 1.5f;
    public static float transferRange = 20f, moveRange = 6f, moveSmoothing = 20f;

    public @Nullable UnitCargoUnloadPointBuild unloadTarget;
    public @Nullable Item itemTarget;
    public float noDestTimer = 0f;
    public int targetIndex = 0;

    @Override
    public void updateMovement(){
        if(!(unit instanceof BuildingTetherc tether) || tether.building() == null) return;

        var build = tether.building();

        if(build.items == null) return;

        //empty, approach the loader, even if there's nothing to pick up (units hanging around doing nothing looks bad)
        if(!unit.hasItem()){
            moveTo(build, moveRange, moveSmoothing);

            //check if ready to pick up
            if(build.items.any() && unit.within(build, transferRange)){
                if(retarget()){
                    findAnyTarget(build);

                    //target has been found, grab items and go
                    if(unloadTarget != null){
                        Call.takeItems(build, itemTarget, Math.min(unit.type.itemCapacity, build.items.get(itemTarget)), unit);
                    }
                }
            }
        }else{ //the unit has an item, deposit it somewhere.

            //there may be no current target, try to find one
            if(unloadTarget == null){
                if(retarget()){
                    findDropTarget(unit.item(), 0, null);

                    //if there is not even a single place to unload, dump items.
                    if(unloadTarget == null){
                        unit.clearItem();
                    }
                }
            }else{

                //what if some prankster reconfigures the source while the unit is moving? we can't have that!
                if(unloadTarget.item != itemTarget){
                    unloadTarget = null;
                    return;
                }

                moveTo(unloadTarget, moveRange, moveSmoothing);

                //deposit in bursts, unloading can take a while
                if(unit.within(unloadTarget, transferRange) && timer.get(timerTarget2, dropSpacing)){
                    int max = unloadTarget.acceptStack(unit.item(), unit.stack.amount, unit);

                    //deposit items when it's possible
                    if(max > 0){
                        noDestTimer = 0f;
                        Call.transferItemTo(unit, unit.item(), max, unit.x, unit.y, unloadTarget);

                        //try the next target later
                        if(!unit.hasItem()){
                            targetIndex ++;
                        }
                    }else if((noDestTimer += dropSpacing) >= emptyWaitTime){
                        //oh no, it's out of space - wait for a while, and if nothing changes, try the next destination

                        //next targeting attempt will try the next destination point
                        targetIndex = findDropTarget(unit.item(), targetIndex, unloadTarget) + 1;

                        //nothing found at all, clear item
                        if(unloadTarget == null){
                            unit.clearItem();
                        }
                    }
                }
            }
        }

    }

    /** find target for the unit's current item */
    public int findDropTarget(Item item, int offset, UnitCargoUnloadPointBuild ignore){
        unloadTarget = null;
        itemTarget = item;

        //autocast for convenience... I know all of these must be cargo unload points anyway
        targets.selectFrom((Seq<UnitCargoUnloadPointBuild>)(Seq)Vars.indexer.getFlagged(unit.team, BlockFlag.unitCargoUnloadPoint), u -> u.item == item);

        if(targets.isEmpty()) return 0;

        UnitCargoUnloadPointBuild lastStale = null;

        offset %= targets.size;

        int i = 0;

        for(var target : targets){
            if(i >= offset && target != ignore){
                if(target.stale){
                    lastStale = target;
                }else{
                    unloadTarget = target;
                    return i;
                }
            }
            i ++;
        }

        //it's still possible that the ignored target may become available at some point, try that, so it doesn't waste items
        if(ignore != null){
            unloadTarget = ignore;
        }else if(lastStale != null){ //a stale target is better than nothing
            unloadTarget = lastStale;
        }

        return -1;
    }

    public void findAnyTarget(Building build){
        unloadTarget = null;
        itemTarget = null;

        //autocast for convenience... I know all of these must be cargo unload points anyway
        var baseTargets = (Seq<UnitCargoUnloadPointBuild>)(Seq)Vars.indexer.getFlagged(unit.team, BlockFlag.unitCargoUnloadPoint);

        if(baseTargets.isEmpty()) return;

        orderedItems.size = 0;
        for(Item item : content.items()){
            if(build.items.get(item) > 0){
                orderedItems.add(item);
            }
        }

        //sort by most items in descending order, and try each one.
        orderedItems.sort(i -> -build.items.get(i));

        UnitCargoUnloadPointBuild lastStale = null;

        outer:
        for(Item item : orderedItems){
            targets.selectFrom(baseTargets, u -> u.item == item);

            if(targets.size > 0) itemTarget = item;

            for(int i = 0; i < targets.size; i ++){
                var target = targets.get((i + targetIndex) % targets.size);

                lastStale = target;

                if(!target.stale){
                    unloadTarget = target;
                    break outer;
                }
            }
        }

        //if the only thing that was found was a "stale" target, at least try that...
        if(unloadTarget == null && lastStale != null){
            unloadTarget = lastStale;
        }
    }

    //unused, might change later
    void sortTargets(Seq<UnitCargoUnloadPointBuild> targets){
        //find sort by "most desirable" first
        targets.sort(Structs.comps(Structs.comparingInt(b -> b.items.total()), Structs.comparingFloat(b -> b.dst2(unit))));
    }
}
