package mindustry.ai.types;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ai.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class CommandAI extends AIController{
    protected static final int maxCommandQueueSize = 50, avoidInterval = 10;
    protected static final Vec2 vecOut = new Vec2(), vecMovePos = new Vec2();
    protected static final boolean[] noFound = {false};
    protected static final UnitPayload tmpPayload = new UnitPayload(null);

    public Seq<Position> commandQueue = new Seq<>(5);
    public @Nullable Vec2 targetPos;
    public @Nullable Teamc attackTarget;
    /** Group of units that were all commanded to reach the same point.. */
    public @Nullable UnitGroup group;
    public int groupIndex = 0;
    /** All encountered unreachable buildings of this AI. Why a sequence? Because contains() is very rarely called on it. */
    public IntSeq unreachableBuildings = new IntSeq(8);
    /** ID of unit read as target. This is set up after reading. Do not access! */
    public int readAttackTarget = -1;

    protected boolean stopAtTarget, stopWhenInRange;
    protected Vec2 lastTargetPos;
    protected boolean blockingUnit;
    protected float timeSpentBlocked;

    /** Stance, usually related to firing mode. */
    public UnitStance stance = UnitStance.shoot;
    /** Current command this unit is following. */
    public UnitCommand command = UnitCommand.moveCommand;
    /** Current controller instance based on command. */
    protected @Nullable AIController commandController;
    /** Last command type assigned. Used for detecting command changes. */
    protected @Nullable UnitCommand lastCommand;

    public UnitCommand currentCommand(){
        return command == null ? UnitCommand.moveCommand : command;
    }

    /** Attempts to assign a command to this unit. If not supported by the unit type, does nothing. */
    public void command(UnitCommand command){
        if(Structs.contains(unit.type.commands, command)){
            //clear old state.
            unit.mineTile = null;
            unit.clearBuilding();
            this.command = command;
        }
    }

    @Override
    public boolean isLogicControllable(){
        return !hasCommand();
    }

    public boolean isAttacking(){
        return target != null && unit.within(target, unit.range() + 10f);
    }

    @Override
    public void updateUnit(){
        //this should not be possible
        if(stance == UnitStance.stop) stance = UnitStance.shoot;

        //pursue the target if relevant
        if(stance == UnitStance.pursueTarget && target != null && attackTarget == null && targetPos == null){
            commandTarget(target, false);
        }

        //remove invalid targets
        if(commandQueue.any()){
            commandQueue.removeAll(e -> e instanceof Healthc h && !h.isValid());
        }

        //assign defaults
        if(command == null && unit.type.commands.length > 0){
            command = unit.type.defaultCommand == null ? unit.type.commands[0] : unit.type.defaultCommand;
        }

        //update command controller based on index.
        var curCommand = command;
        if(lastCommand != curCommand){
            lastCommand = curCommand;
            commandController = (curCommand == null ? null : curCommand.controller.get(unit));
        }

        //use the command controller if it is provided, and bail out.
        if(commandController != null){
            if(commandController.unit() != unit) commandController.unit(unit);
            commandController.updateUnit();
        }else{
            defaultBehavior();
            //boosting control is not supported, so just don't.
            unit.updateBoosting(false);
        }
    }

    public void clearCommands(){
        commandQueue.clear();
        targetPos = null;
        attackTarget = null;
    }

    public void defaultBehavior(){

        if(!net.client() && unit instanceof Payloadc pay){
            //auto-drop everything
            if(command == UnitCommand.unloadPayloadCommand && pay.hasPayload()){
                Call.payloadDropped(unit, unit.x, unit.y);
            }

            //try to pick up what's under it
            if(command == UnitCommand.loadUnitsCommand){
                Unit target = Units.closest(unit.team, unit.x, unit.y, unit.type.hitSize * 2f, u -> u.isAI() && u != unit && u.isGrounded() && pay.canPickup(u) && u.within(unit, u.hitSize + unit.hitSize));
                if(target != null){
                    Call.pickedUnitPayload(unit, target);
                }
            }

            //try to pick up a block
            if(command == UnitCommand.loadBlocksCommand && (targetPos == null || unit.within(targetPos, 1f))){
                Building build = world.buildWorld(unit.x, unit.y);

                if(build != null && state.teams.canInteract(unit.team, build.team)){
                    //pick up block's payload
                    Payload current = build.getPayload();
                    if(current != null && pay.canPickupPayload(current)){
                        Call.pickedBuildPayload(unit, build, false);
                        //pick up whole building directly
                    }else if(build.block.buildVisibility != BuildVisibility.hidden && build.canPickup() && pay.canPickup(build)){
                        Call.pickedBuildPayload(unit, build, true);
                    }
                }
            }
        }

        if(!net.client() && command == UnitCommand.enterPayloadCommand && unit.buildOn() != null && (targetPos == null || (world.buildWorld(targetPos.x, targetPos.y) != null && world.buildWorld(targetPos.x, targetPos.y) == unit.buildOn()))){
            var build = unit.buildOn();
            tmpPayload.unit = unit;
            if(build.team == unit.team && build.acceptPayload(build, tmpPayload)){
                Call.unitEnteredPayload(unit, build);
                return; //no use updating after this, the unit is gone!
            }
        }

        //acquiring naval targets isn't supported yet, so use the fallback dumb AI
        if(unit.team.isAI() && unit.team.rules().rtsAi && unit.type.naval){
            if(fallback == null) fallback = new GroundAI();

            if(fallback.unit() != unit) fallback.unit(unit);
            fallback.updateUnit();
            return;
        }

        updateVisuals();
        //only autotarget if the unit supports it
        if((targetPos == null || nearAttackTarget(unit.x, unit.y, unit.range())) || unit.type.autoFindTarget){
            updateTargeting();
        }else if(attackTarget == null){
            //if the unit does not have an attack target, is currently moving, and does not have autotargeting, stop attacking stuff
            target = null;
            for(var mount : unit.mounts){
                if(mount.weapon.controllable){
                    mount.target = null;
                }
            }
        }

        if(attackTarget != null && invalid(attackTarget)){
            attackTarget = null;
            targetPos = null;
        }

        //move on to the next target
        if(attackTarget == null && targetPos == null){
            finishPath();
        }

        if(attackTarget != null){
            if(targetPos == null){
                targetPos = new Vec2();
                lastTargetPos = targetPos;
            }
            targetPos.set(attackTarget);

            if(unit.isGrounded() && attackTarget instanceof Building build && build.tile.solid() && unit.pathType() != Pathfinder.costLegs && stance != UnitStance.ram){
                Tile best = build.findClosestEdge(unit, Tile::solid);
                if(best != null){
                    targetPos.set(best);
                }
            }
        }

        boolean alwaysArrive = false;

        float engageRange = unit.type.range - 10f;
        boolean withinAttackRange = attackTarget != null && unit.within(attackTarget, engageRange) && stance != UnitStance.ram;

        if(targetPos != null){
            boolean move = true, isFinalPoint = commandQueue.size == 0;
            vecOut.set(targetPos);
            vecMovePos.set(targetPos);

            //the enter payload command requires an exact position
            if(group != null && group.valid && groupIndex < group.units.size && command != UnitCommand.enterPayloadCommand){
                vecMovePos.add(group.positions[groupIndex * 2], group.positions[groupIndex * 2 + 1]);
            }

            //TODO: should the unit stop when it finds a target?
            if(stance == UnitStance.patrol && target != null && unit.within(target, unit.type.range - 2f) && !unit.type.circleTarget){
                move = false;
            }

            if(unit.isGrounded() && stance != UnitStance.ram){
                //TODO: blocking enable or disable?
                if(timer.get(timerTarget3, avoidInterval)){
                    Vec2 dstPos = Tmp.v1.trns(unit.rotation, unit.hitSize/2f);
                    float max = unit.hitSize/2f;
                    float radius = Math.max(7f, max);
                    float margin = 4f;
                    blockingUnit = Units.nearbyCheck(unit.x + dstPos.x - radius/2f, unit.y + dstPos.y - radius/2f, radius, radius,
                        u -> u != unit && u.within(unit, u.hitSize/2f + unit.hitSize/2f + margin) && u.controller() instanceof CommandAI ai && ai.targetPos != null &&
                        //stop for other unit only if it's closer to the target
                        (ai.targetPos.equals(targetPos) && u.dst2(targetPos) < unit.dst2(targetPos)) &&
                        //don't stop if they're facing the same way
                        !Angles.within(unit.rotation, u.rotation, 15f) &&
                        //must be near an obstacle, stopping in open ground is pointless
                        ControlPathfinder.isNearObstacle(unit, unit.tileX(), unit.tileY(), u.tileX(), u.tileY()));
                }

                float maxBlockTime = 60f * 5f;

                if(blockingUnit){
                    timeSpentBlocked += Time.delta;

                    if(timeSpentBlocked >= maxBlockTime*2f){
                        timeSpentBlocked = 0f;
                    }
                }else{
                    timeSpentBlocked = 0f;
                }

                //if the unit is next to the target, stop asking the pathfinder how to get there, it's a waste of CPU
                //TODO maybe stop moving too?
                if(withinAttackRange){
                    move = true;
                    noFound[0] = false;
                    vecOut.set(vecMovePos);
                }else{
                    move = controlPath.getPathPosition(unit, vecMovePos, targetPos, vecOut, noFound) && (!blockingUnit || timeSpentBlocked > maxBlockTime);
                }

                //rare case where unit must be perfectly aligned (happens with 1-tile gaps)
                alwaysArrive = vecOut.epsilonEquals(unit.tileX() * tilesize, unit.tileY() * tilesize);
                //we've reached the final point if the returned coordinate is equal to the supplied input
                isFinalPoint &= vecMovePos.epsilonEquals(vecOut, 4.1f);

                //if the path is invalid, stop trying and record the end as unreachable
                if(unit.team.isAI() && (noFound[0] || unit.isPathImpassable(World.toTile(vecMovePos.x), World.toTile(vecMovePos.y)))){
                    if(attackTarget instanceof Building build){
                        unreachableBuildings.addUnique(build.pos());
                    }
                    attackTarget = null;
                    finishPath();
                    return;
                }
            }else{
                vecOut.set(vecMovePos);
            }

            if(move){
                if(unit.type.circleTarget && attackTarget != null){
                    target = attackTarget;
                    circleAttack(80f);
                }else{
                    moveTo(vecOut,
                    withinAttackRange ? engageRange :
                    unit.isGrounded() ? 0f :
                    attackTarget != null && stance != UnitStance.ram ? engageRange : 0f,
                    unit.isFlying() ? 40f : 100f, false, null, isFinalPoint || alwaysArrive);
                }
            }

            //if stopAtTarget is set, stop trying to move to the target once it is reached - used for defending
            if(attackTarget != null && stopAtTarget && unit.within(attackTarget, engageRange - 1f)){
                attackTarget = null;
            }

            if(unit.isFlying() && move && (attackTarget == null || !unit.within(attackTarget, unit.type.range))){
                unit.lookAt(vecMovePos);
            }else{
                faceTarget();
            }

            //reached destination, end pathfinding
            if(attackTarget == null && unit.within(vecMovePos, command.exactArrival && commandQueue.size == 0 ? 1f : Math.max(5f, unit.hitSize / 2f))){
                finishPath();
            }

            if(stopWhenInRange && targetPos != null && unit.within(vecMovePos, engageRange * 0.9f)){
                finishPath();
                stopWhenInRange = false;
            }

        }else if(target != null){
            faceTarget();
        }
    }

    void finishPath(){
        //the enter payload command never finishes until they are actually accepted
        if(command == UnitCommand.enterPayloadCommand && commandQueue.size == 0 && targetPos != null && world.buildWorld(targetPos.x, targetPos.y) != null && world.buildWorld(targetPos.x, targetPos.y).block.acceptsPayloads){
            return;
        }

        Vec2 prev = targetPos;
        targetPos = null;

        if(commandQueue.size > 0){
            var next = commandQueue.remove(0);
            if(next instanceof Teamc target){
                commandTarget(target, this.stopAtTarget);
            }else if(next instanceof Vec2 position){
                commandPosition(position);
            }

            if(prev != null && stance == UnitStance.patrol){
                commandQueue.add(prev.cpy());
            }

            //make sure spot in formation is reachable
            if(group != null){
                group.updateRaycast(groupIndex, next instanceof Vec2 position ? position : Tmp.v3.set(next));
            }
        }else{
            if(group != null){
                group = null;
            }
        }
    }

    public void commandQueue(Position location){
        if(targetPos == null && attackTarget == null){
            if(location instanceof Teamc target){
                commandTarget(target, this.stopAtTarget);
            }else if(location instanceof Vec2 position){
                commandPosition(position);
            }
        }else if(commandQueue.size < maxCommandQueueSize && !commandQueue.contains(location)){
            commandQueue.add(location);
        }
    }

    @Override
    public void afterRead(Unit unit){
        if(readAttackTarget != -1){
            attackTarget = Groups.unit.getByID(readAttackTarget);
            readAttackTarget = -1;
        }
    }

    @Override
    public boolean shouldFire(){
        return stance != UnitStance.holdFire;
    }

    @Override
    public void hit(Bullet bullet){
        if(unit.team.isAI() && bullet.owner instanceof Teamc teamc && teamc.team() != unit.team && attackTarget == null &&
            //can only counter-attack every few seconds to prevent rapidly changing targets
            !(teamc instanceof Unit u && !u.checkTarget(unit.type.targetAir, unit.type.targetGround)) && timer.get(timerTarget4, 60f * 10f)){
            commandTarget(teamc, true);
        }
    }

    @Override
    public boolean keepState(){
        return true;
    }

    @Override
    public Teamc findTarget(float x, float y, float range, boolean air, boolean ground){
        return !nearAttackTarget(x, y, range) ? super.findTarget(x, y, range, air, ground) : attackTarget;
    }

    public boolean nearAttackTarget(float x, float y, float range){
        return attackTarget != null && attackTarget.within(x, y, range + 3f + (attackTarget instanceof Sized s ? s.hitSize()/2f : 0f));
    }

    @Override
    public boolean retarget(){
        //retarget faster when there is an explicit target
        return attackTarget != null ? timer.get(timerTarget, 10) : timer.get(timerTarget, 20);
    }

    public boolean hasCommand(){
        return targetPos != null;
    }

    public void setupLastPos(){
        lastTargetPos = targetPos;
    }

    @Override
    public void commandPosition(Vec2 pos){
        if(pos == null) return;

        commandPosition(pos, false);
        if(commandController != null){
            commandController.commandPosition(pos);
        }
    }

    public void commandPosition(Vec2 pos, boolean stopWhenInRange){
        if(pos == null) return;

        //this is an allocation, but it's relatively rarely called anyway, and outside mutations must be prevented
        targetPos = lastTargetPos = pos.cpy();
        attackTarget = null;
        this.stopWhenInRange = stopWhenInRange;
    }

    @Override
    public void commandTarget(Teamc moveTo){
        commandTarget(moveTo, false);
        if(commandController != null){
            commandController.commandTarget(moveTo);
        }
    }

    public void commandTarget(Teamc moveTo, boolean stopAtTarget){
        attackTarget = moveTo;
        this.stopAtTarget = stopAtTarget;
    }

    /*

    //TODO ひどい
    (does not work)

    public static float cohesionScl = 0.3f;
    public static float cohesionRad = 3f, separationRad = 1.1f, separationScl = 1f, flockMult = 0.5f;

    Vec2 calculateFlock(){
        if(local.isEmpty()) return flockVec.setZero();

        flockVec.setZero();
        separation.setZero();
        cohesion.setZero();
        massCenter.set(unit);

        float rad = unit.hitSize;
        float sepDst = rad * separationRad, cohDst = rad * cohesionRad;

        //"cohesed" isn't even a word smh
        int separated = 0, cohesed = 1;

        for(var other : local){
            float dst = other.dst(unit);
            if(dst < sepDst){
                separation.add(Tmp.v1.set(unit).sub(other).scl(1f / sepDst));
                separated ++;
            }

            if(dst < cohDst){
                massCenter.add(other);
                cohesed ++;
            }
        }

        if(separated > 0){
            separation.scl(1f / separated);
            flockVec.add(separation.scl(separationScl));
        }

        if(cohesed > 1){
            massCenter.scl(1f / cohesed);
            flockVec.add(Tmp.v1.set(massCenter).sub(unit).limit(cohesionScl * unit.type.speed));
            //seek mass center?
        }

        return flockVec;
    }*/
}
