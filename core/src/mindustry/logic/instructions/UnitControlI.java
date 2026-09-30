package mindustry.logic.instructions;

import arc.math.*;
import arc.util.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/** Controls the unit based on some parameters. */
public class UnitControlI implements LogicInstruction{
    public LogicUnitControl type = LogicUnitControl.move;
    public LogicVar p1, p2, p3, p4, p5;

    public UnitControlI(LogicUnitControl type, LogicVar p1, LogicVar p2, LogicVar p3, LogicVar p4, LogicVar p5){
        this.type = type;
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
        this.p4 = p4;
        this.p5 = p5;
    }

    public UnitControlI(){
    }

    /** Checks is a unit is valid for logic AI control, and returns the controller. */
    public static @Nullable LogicAI checkLogicAI(LogicExecutor exec, Object unitObj, boolean control){
        if(unitObj instanceof Unit unit && unit.isValid() && exec.unit.obj() == unit && (unit.team == exec.team || exec.privileged) && unit.controller().isLogicControllable()){
            if(unit.controller() instanceof LogicAI la){
                la.controller = exec.thisv.building();
                return la;
            }else if(control){
                var la = new LogicAI();
                la.controller = exec.thisv.building();

                unit.controller(la);
                //clear old state
                unit.mineTile = null;
                unit.clearBuilding();

                return la;
            }
        }
        return null;
    }

    @Override
    public void run(LogicExecutor exec){
        if(!exec.privileged && !state.rules.logicUnitControl) return;

        Object unitObj = exec.unit.obj();
        boolean control = type != LogicUnitControl.unbind && type != LogicUnitControl.within;
        LogicAI ai = checkLogicAI(exec, unitObj, control);

        //only control standard AI units
        if(unitObj instanceof Unit unit && (ai != null || !control)){
            if(ai != null) ai.controlTimer = LogicAI.logicControlTimeout;
            float x1 = World.unconv(p1.numf()), y1 = World.unconv(p2.numf()), d1 = World.unconv(p3.numf());

            switch(type){
                case idle, autoPathfind -> {
                    ai.control = type;
                }
                case move, stop, approach, pathfind -> {
                    ai.control = type;
                    ai.moveX = x1;
                    ai.moveY = y1;
                    if(type == LogicUnitControl.approach){
                        ai.moveRad = d1;
                    }

                    //stop mining/building
                    if(type == LogicUnitControl.stop){
                        unit.mineTile = null;
                        unit.clearBuilding();
                    }
                }
                case unbind -> {
                    if(unit.controller() instanceof LogicAI){
                        unit.resetController();
                    }
                }
                case within -> {
                    p4.setnum(unit.within(x1, y1, d1) ? 1 : 0);
                }
                case target -> {
                    ai.posTarget.set(x1, y1);
                    ai.aimControl = type;
                    ai.mainTarget = null;
                    ai.shoot = p3.bool();
                }
                case targetp -> {
                    ai.aimControl = type;
                    ai.mainTarget = p1.obj() instanceof Teamc t ? t : null;
                    ai.shoot = p2.bool();
                }
                case boost -> {
                    ai.boost = p1.bool();
                }
                case flag -> {
                    unit.flag = p1.num();
                }
                case mine -> {
                    Tile tile = state.world.tileWorld(x1, y1);
                    if(unit.canMine()){
                        unit.mineTile = unit.validMine(tile) ? tile : null;
                    }
                }
                case payDrop -> {
                    if(!exec.timeoutDone(unit, LogicAI.transferDelay)) return;

                    if(unit instanceof Payloadc pay && pay.hasPayload()){
                        Call.payloadDropped(unit, unit.x, unit.y);
                        exec.updateTimeout(unit);
                    }
                }
                case payTake -> {
                    if(!exec.timeoutDone(unit, LogicAI.transferDelay)) return;

                    if(unit instanceof Payloadc pay){
                        //units
                        if(p1.bool()){
                            Unit result = Units.closest(unit.team, unit.x, unit.y, unit.type.hitSize * 2f, u -> u != unit && u.isAI() && u.isGrounded() && pay.canPickup(u) && u.within(unit, u.hitSize + unit.hitSize * 1.2f));

                            if(result != null){
                                Call.pickedUnitPayload(unit, result);
                            }
                        }else{ //buildings
                            Building build = state.world.buildWorld(unit.x, unit.y);

                            //TODO copy pasted code
                            if(build != null && build.team == unit.team){
                                Payload current = build.getPayload();
                                if(current != null && pay.canPickupPayload(current)){
                                    Call.pickedBuildPayload(unit, build, false);
                                    //pick up whole building directly
                                }else if(build.block.buildVisibility != BuildVisibility.hidden && build.canPickup() && pay.canPickup(build)){
                                    Call.pickedBuildPayload(unit, build, true);
                                }
                            }
                        }
                        exec.updateTimeout(unit);
                    }
                }
                case payEnter -> {
                    Building build = state.world.buildWorld(unit.x, unit.y);
                    if(build != null && unit.team() == build.team && build.canControlSelect(unit)){
                        Call.unitBuildingControlSelect(unit, build);
                    }
                }
                case build -> {
                    if((state.rules.logicUnitBuild || exec.privileged) && unit.canBuild() && p3.obj() instanceof Block block && block.canBeBuilt() && (block.unlockedNow() || unit.team.isAI())){
                        int x = World.toTile(x1 - block.offset / tilesize), y = World.toTile(y1 - block.offset / tilesize);
                        int rot = Mathf.mod(p4.numi(), 4);

                        //reset state of last request when necessary
                        if(ai.plan.x != x || ai.plan.y != y || ai.plan.block != block || unit.plans.isEmpty()){
                            ai.plan.progress = 0;
                            ai.plan.initialized = false;
                            ai.plan.stuck = false;
                        }

                        var conf = p5.obj();
                        ai.plan.set(x, y, rot, block);
                        ai.plan.config = conf instanceof Content c ? c : conf instanceof Building b ? b : null;

                        unit.clearBuilding();
                        Tile tile = ai.plan.tile();

                        if(tile != null && !(tile.block() == block && tile.build != null && tile.build.rotation == rot)){
                            unit.updateBuilding = true;
                            unit.addBuild(ai.plan);
                        }
                    }
                }
                case deconstruct -> {
                    if((state.rules.logicUnitDeconstruct || exec.privileged) && unit.canBuild()){
                        //reset state of last request when necessary
                        if(ai.plan.x != World.toTile(x1) || ai.plan.y != World.toTile(y1) || !ai.plan.breaking || unit.plans.isEmpty()){
                            ai.plan.progress = 0;
                            ai.plan.initialized = false;
                            ai.plan.stuck = false;
                        }

                        ai.plan.x = World.toTile(x1);
                        ai.plan.y = World.toTile(y1);
                        ai.plan.breaking = true;

                        unit.clearBuilding();
                        Tile tile = ai.plan.tile();

                        if(tile != null && Build.validBreak(unit.team, ai.plan.x, ai.plan.y)){
                            unit.updateBuilding = true;
                            unit.addBuild(ai.plan);
                        }
                    }
                }
                case getBlock -> {
                    float range = Math.max(unit.range(), unit.type.buildRange);
                    if(!unit.within(x1, y1, range)){
                        p3.setobj(null);
                        p4.setobj(null);
                        p5.setobj(null);
                    }else{
                        Tile tile = state.world.tileWorld(x1, y1);
                        if(tile == null){
                            p3.setobj(null);
                            p4.setobj(null);
                            p5.setobj(null);
                        }else{
                            p3.setobj(tile.block());
                            p4.setobj(tile.build != null ? tile.build : null);
                            //Allows reading of ore tiles if they are present (overlay is not air) otherwise returns the floor
                            p5.setobj(tile.overlay() == Blocks.air ? tile.floor() : tile.overlay());
                        }
                    }
                }
                case itemDrop -> {
                    if(p1.obj() != Blocks.air && !exec.timeoutDone(unit, LogicAI.transferDelay)) return;

                    //clear item when dropping to @air
                    if(p1.obj() == Blocks.air){
                        //only server-side; no need to call anything, as items are synced in snapshots
                        if(!net.client()){
                            unit.clearItem();
                        }
                    }else{
                        Building build = p1.building();
                        int dropped = Math.min(unit.stack.amount, p2.numi());
                        if(build != null && build.team == unit.team && build.isValid() && build.allowDeposit() && dropped > 0 && unit.within(build, logicItemTransferRange + build.block.size * tilesize / 2f)){
                            int accepted = build.acceptStack(unit.item(), dropped, unit);
                            if(accepted > 0){
                                Call.transferItemTo(unit, unit.item(), accepted, unit.x, unit.y, build);
                                exec.updateTimeout(unit);
                            }
                        }
                    }
                }
                case itemTake -> {
                    if(!exec.timeoutDone(unit, LogicAI.transferDelay)) return;

                    Building build = p1.building();
                    int amount = p3.numi();

                    if(build != null && build.team == unit.team && build.isValid() && build.items != null &&
                    p2.obj() instanceof Item item && unit.within(build, logicItemTransferRange + build.block.size * tilesize / 2f)){
                        int taken = Math.min(build.items.get(item), Math.min(amount, unit.maxAccepted(item)));

                        if(taken > 0){
                            Call.takeItems(build, item, taken, unit);
                            exec.updateTimeout(unit);
                        }
                    }
                }
                default -> {
                }
            }
        }
    }
}
