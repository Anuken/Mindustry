package mindustry.ai.types;

import arc.math.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class GroundAI extends AIController{
    float stuckTime = 0f;
    float stuckX = -999f, stuckY = -999f;

    static final float stuckRange = tilesize * 1.5f;

    @Override
    public void updateMovement(){

        //if it hasn't moved the stuck range in twice the time it should have taken, it's stuck
        float stuckThreshold = Math.max(1f, stuckRange * 2f / unit.type.speed);

        Building core = unit.closestEnemyCore();
        boolean moved = false;

        if(core != null && unit.within(core, unit.range() / 1.3f + core.block.size * tilesize / 2f)){
            target = core;
            for(var mount : unit.mounts){
                if(mount.weapon.controllable && mount.weapon.bullet.collidesGround){
                    mount.target = core;
                }
            }
        }

        if((core == null || !unit.within(core, unit.type.range * 0.5f))){
            boolean move = true;

            if(state.rules.waves && unit.team == state.rules.defaultTeam){
                Tile spawner = getClosestSpawner();
                if(spawner != null && unit.within(spawner, state.rules.dropZoneRadius + 120f)) move = false;
                if(spawner == null && core == null) move = false;
            }

            //no reason to move if there's nothing there
            if(core == null && (!state.rules.waves || getClosestSpawner() == null)){
                move = false;
            }

            moved = move;

            if(move) pathfind(Pathfinder.fieldCore, true, stuckTime >= stuckThreshold);
        }

        if(unit.type.canBoost && unit.elevation > 0.001f && !unit.onSolid()){
            unit.elevation = Mathf.approachDelta(unit.elevation, 0f, unit.type.riseSpeed);
        }

        faceTarget();

        if(moved){

            if(unit.within(stuckX, stuckY, stuckRange)){
                stuckTime += Time.delta;
                if(stuckTime - Time.delta < stuckThreshold && stuckTime >= stuckThreshold){
                    float radius = unit.hitSize * Vars.unitCollisionRadiusScale * 2f;
                    Units.nearby(unit.team, unit.x, unit.y, radius, other -> {
                        if(other != unit && other.controller() instanceof GroundAI ai && other.within(unit.x, unit.y, radius + other.hitSize * unitCollisionRadiusScale)){
                            ai.stuckX = other.x;
                            ai.stuckY = other.y;
                            ai.stuckTime = Math.max(1f, stuckRange * 2f / other.type.speed) + 1f;
                        }
                    });
                }
            }else{
                stuckX = unit.x;
                stuckY = unit.y;
                stuckTime = 0f;
            }
        }else{
            stuckTime = 0f;
        }
    }
}
