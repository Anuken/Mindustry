package mindustry.ai.types;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.logic.*;

public class LogicAI extends AIController{
    /** Minimum delay between item transfers. */
    public static final float transferDelay = 60f * 1.5f;
    /** Time after which the unit resets its controlled and reverts to a normal unit. */
    public static final float logicControlTimeout = 60f * 10f;

    public LUnitControl control = LUnitControl.idle;
    public float moveX, moveY, moveRad;
    public float controlTimer = logicControlTimeout, targetTimer;
    @Nullable
    public Building controller;
    public BuildPlan plan = new BuildPlan();

    //special cache for instruction to store data
    public ObjectMap<Object, Object> execCache = new ObjectMap<>();

    //type of aiming to use
    public LUnitControl aimControl = LUnitControl.stop;

    //whether to use the boost (certain units only)
    public boolean boost;
    //main target set for shootP
    public Teamc mainTarget;
    //whether to shoot at all
    public boolean shoot;
    //target shoot positions for manual aiming
    public PosTeam posTarget = PosTeam.create();

    private ObjectSet<Object> radars = new ObjectSet<>();
    private float lastMoveX, lastMoveY;
    private int lastPathId = 0;

    @Override
    public void updateMovement(){
        if(control == LUnitControl.pathfind){
            if(!Mathf.equal(moveX, lastMoveX, 0.1f) || !Mathf.equal(moveY, lastMoveY, 0.1f)){
                lastPathId ++;
                lastMoveX = moveX;
                lastMoveY = moveY;
            }
        }

        if(targetTimer > 0f){
            targetTimer -= Time.delta;
        }else{
            radars.clear();
            targetTimer = 40f;
        }

        //timeout when not controlled by logic for a while
        if(controlTimer > 0 && controller != null && controller.isValid()){
            controlTimer -= Time.delta;
        }else{
            unit.resetController();
            return;
        }

        switch(control){
            case move -> {
                moveTo(Tmp.v1.set(moveX, moveY), 1f, 30f);
            }
            case approach -> {
                moveTo(Tmp.v1.set(moveX, moveY), moveRad - 7f, 7, true, null);
            }
            case pathfind -> {
                if(unit.isFlying()){
                    moveTo(Tmp.v1.set(moveX, moveY), 1f, 30f);
                }else{
                    if(Vars.controlPath.getPathPosition(unit, lastPathId, Tmp.v2.set(moveX, moveY), Tmp.v1, null)){
                        moveTo(Tmp.v1, 1f, Tmp.v2.epsilonEquals(Tmp.v1, 4.1f) ? 30f : 0f);
                    }
                }
            }
            case stop -> {
                unit.clearBuilding();
            }
        }

        if(unit.type.canBoost && !unit.type.flying){
            unit.elevation = Mathf.approachDelta(unit.elevation, Mathf.num(boost || unit.onSolid() || (unit.isFlying() && !unit.canLand())), unit.type.riseSpeed);
        }

        //look where moving if there's nothing to aim at
        if(!shoot || !unit.type.omniMovement){
            unit.lookAt(unit.prefRotation());
        }else if(unit.hasWeapons() && unit.mounts.length > 0 && !unit.mounts[0].weapon.ignoreRotation){ //if there is, look at the object
            unit.lookAt(unit.mounts[0].aimX, unit.mounts[0].aimY);
        }
    }

    public boolean checkTargetTimer(Object radar){
        return radars.add(radar);
    }

    @Override
    public boolean checkTarget(Teamc target, float x, float y, float range){
        return false;
    }

    //always retarget
    @Override
    public boolean retarget(){
        return true;
    }

    @Override
    public boolean invalid(Teamc target){
        return false;
    }

    @Override
    public boolean shouldShoot(){
        return shoot && !(unit.type.canBoost && boost);
    }

    //always aim for the main target
    @Override
    public Teamc target(float x, float y, float range, boolean air, boolean ground){
        return switch(aimControl){
            case target -> posTarget;
            case targetp -> mainTarget;
            default -> null;
        };
    }
}
