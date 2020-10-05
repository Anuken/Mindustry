package mindustry.ai.types;

import arc.struct.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.logic.LExecutor.*;
import mindustry.logic.*;

public class LogicAI extends AIController{
    /** Minimum delay between item transfers. */
    public static final float transferDelay = 60f * 2f;
    /** Time after which the unit resets its controlled and reverts to a normal unit. */
    public static final float logicControlTimeout = 15f * 60f;

    public LUnitControl control = LUnitControl.stop;
    public float moveX, moveY, moveRad;
    public float itemTimer, controlTimer = logicControlTimeout, targetTimer;

    //type of aiming to use
    public LUnitControl aimControl = LUnitControl.stop;

    //main target set for shootP
    public Teamc mainTarget;
    //whether to shoot at all
    public boolean shoot;
    //target shoot positions for manual aiming
    public PosTeam posTarget = PosTeam.create();

    private ObjectSet<RadarI> radars = new ObjectSet<>();

    @Override
    protected void updateMovement(){
        if(itemTimer > 0){
            itemTimer -= Time.delta;
        }

        if(targetTimer > 0f){
            targetTimer -= Time.delta;
        }else{
            radars.clear();
            targetTimer = 30f;
        }

        //timeout when not controlled by logic for a while
        if(controlTimer > 0){
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
                moveTo(Tmp.v1.set(moveX, moveY), moveRad, 10f);
            }
        }

        //look where moving if there's nothing to aim at
        if(!shoot){
            if(unit.moving()){
                unit.lookAt(unit.vel().angle());
            }
        }else if(unit.hasWeapons()){ //if there is, look at the object
            unit.lookAt(unit.mounts[0].aimX, unit.mounts[0].aimY);
        }
    }

    public boolean checkTargetTimer(RadarI radar){
        return radars.add(radar);
    }

    //always retarget
    @Override
    protected boolean retarget(){
        return true;
    }

    @Override
    protected boolean invalid(Teamc target){
        return false;
    }

    @Override
    protected boolean shouldShoot(){
        return shoot;
    }

    //always aim for the main target
    @Override
    protected Teamc target(float x, float y, float range, boolean air, boolean ground){
        return switch(aimControl){
            case target -> posTarget;
            case targetp -> mainTarget;
            default -> null;
        };
    }
}
