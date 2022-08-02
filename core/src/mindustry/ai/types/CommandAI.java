package mindustry.ai.types;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;

public class CommandAI extends AIController{
    protected static final float localInterval = 40f;
    protected static final Vec2 vecOut = new Vec2(), flockVec = new Vec2(), separation = new Vec2(), cohesion = new Vec2(), massCenter = new Vec2();

    public @Nullable Vec2 targetPos;
    public @Nullable Teamc attackTarget;

    protected boolean stopAtTarget;
    protected Vec2 lastTargetPos;
    protected int pathId = -1;
    protected Seq<Unit> local = new Seq<>(false);
    protected boolean flocked;

    /** Current command this unit is following. */
    public @Nullable UnitCommand command;
    /** Current controller instance based on command. */
    protected @Nullable AIController commandController;
    /** Last command type assigned. Used for detecting command changes. */
    protected @Nullable UnitCommand lastCommand;

    public @Nullable UnitCommand currentCommand(){
        return command;
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
    public void updateUnit(){

        //assign defaults
        if(command == null && unit.type.commands.length > 0){
            command = unit.type.defaultCommand == null ? unit.type.commands[0] : unit.type.defaultCommand;
        }

        //update command controller based on index.
        var curCommand = currentCommand();
        if(lastCommand != curCommand){
            lastCommand = curCommand;
            commandController = (curCommand == null ? null : curCommand.controller.get(unit));
        }

        //use the command controller if it is provided, and bail out.
        if(commandController != null){
            if(commandController.unit() != unit) commandController.unit(unit);
            commandController.updateUnit();
            return;
        }

        updateVisuals();
        updateTargeting();

        if(attackTarget != null && invalid(attackTarget)){
            attackTarget = null;
            targetPos = null;
        }

        if(targetPos != null){
            if(timer.get(timerTarget3, localInterval) || !flocked){
                if(!flocked){
                    //make sure updates are staggered randomly
                    timer.reset(timerTarget3, Mathf.random(localInterval));
                }

                local.clear();
                //TODO experiment with 2/3/4
                float size = unit.hitSize * 3f;
                unit.team.data().tree().intersect(unit.x - size / 2f, unit.y - size/2f, size, size, local);
                local.remove(unit);
                flocked = true;
            }
        }else{
            flocked = false;
        }

        if(attackTarget != null){
            if(targetPos == null){
                targetPos = new Vec2();
                lastTargetPos = targetPos;
            }
            targetPos.set(attackTarget);

            if(unit.isGrounded() && attackTarget instanceof Building build && build.tile.solid() && unit.pathType() != Pathfinder.costLegs){
                Tile best = build.findClosestEdge(unit, Tile::solid);
                if(best != null){
                    targetPos.set(best);
                }
            }
        }

        if(targetPos != null){
            boolean move = true;
            vecOut.set(targetPos);

            if(unit.isGrounded()){
                move = Vars.controlPath.getPathPosition(unit, pathId, targetPos, vecOut);
            }

            float engageRange = unit.type.range - 10f;

            if(move){
                if(unit.type.circleTarget && attackTarget != null){
                    target = attackTarget;
                    circleAttack(80f);
                }else{
                    moveTo(vecOut,
                        attackTarget != null && unit.within(attackTarget, engageRange) ? engageRange :
                        unit.isGrounded() ? 0f :
                        attackTarget != null ? engageRange :
                        0f, unit.isFlying() ? 40f : 100f, false, null, true);
                }

                //calculateFlock().limit(unit.speed() * flockMult)
            }

            //if stopAtTarget is set, stop trying to move to the target once it is reached - used for defending
            if(attackTarget != null && stopAtTarget && unit.within(attackTarget, engageRange - 1f)){
                attackTarget = null;
            }

            if(unit.isFlying()){
                unit.lookAt(targetPos);
            }else{
                faceTarget();
            }

            if(attackTarget == null){
                //TODO overshoot.
                if(unit.within(targetPos, Math.max(5f, unit.hitSize / 2f))){
                    targetPos = null;
                }else if(local.size > 1){
                    int count = 0;
                    for(var near : local){
                        //has arrived - no current command, but last one is equal
                        if(near.isCommandable() && !near.command().hasCommand() && targetPos.epsilonEquals(near.command().lastTargetPos, 0.001f)){
                            count ++;
                        }
                    }

                    //others have arrived at destination, so this one will too
                    if(count >= Math.max(3, local.size / 2)){
                        targetPos = null;
                    }
                }
            }

        }else if(target != null){
            faceTarget();
        }
    }

    @Override
    public void hit(Bullet bullet){
        if(unit.team.isAI() && bullet.owner instanceof Teamc teamc && teamc.team() != unit.team && attackTarget == null && !(teamc instanceof Unit u && !u.checkTarget(unit.type.targetAir, unit.type.targetGround))){
            commandTarget(teamc, true);
        }
    }

    @Override
    public boolean keepState(){
        return true;
    }

    @Override
    public Teamc findTarget(float x, float y, float range, boolean air, boolean ground){
        return attackTarget == null || !attackTarget.within(x, y, range + 3f + (attackTarget instanceof Sized s ? s.hitSize()/2f : 0f)) ? super.findTarget(x, y, range, air, ground) : attackTarget;
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

    public void commandPosition(Vec2 pos){
        targetPos = pos;
        lastTargetPos = pos;
        attackTarget = null;
        pathId = Vars.controlPath.nextTargetId();
    }

    public void commandTarget(Teamc moveTo){
        commandTarget(moveTo, false);
    }

    public void commandTarget(Teamc moveTo, boolean stopAtTarget){
        attackTarget = moveTo;
        this.stopAtTarget = stopAtTarget;
        pathId = Vars.controlPath.nextTargetId();
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
