package mindustry.ai.types;

import arc.math.*;
import arc.math.geom.*;
import mindustry.ai.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class HugAI extends AIController{

    @Override
    public void updateMovement(){

        Building core = unit.closestEnemyCore();

        if(core != null && unit.within(core, unit.range() / 1.1f + core.block.size * tilesize / 2f)){
            target = core;
            for(var mount : unit.mounts){
                if(mount.weapon.controllable && mount.weapon.bullet.collidesGround){
                    mount.target = core;
                }
            }
        }

        boolean move = true;

        if(state.rules.waves && unit.team == state.rules.defaultTeam){
            Tile spawner = getClosestSpawner();
            if(spawner != null && unit.within(spawner, state.rules.dropZoneRadius + 120f)) move = false;
        }

        //raycast for target
        if(target != null && unit.within(target, unit.type.range) && !World.raycast(unit.tileX(), unit.tileY(), target.tileX(), target.tileY(), (x, y) -> {
            for(Point2 p : Geometry.d4c){
                if(!unit.canPass(x + p.x, y + p.y)){
                    return true;
                }
            }
            return false;
        })){
            if(unit.within(target, (unit.hitSize + (target instanceof Sized s ? s.hitSize() : 1f)) * 0.5f)){
                //circle target
                unit.movePref(vec.set(target).sub(unit).rotate(90f).setLength(unit.speed()));
            }else{
                //move toward target in a straight line
                unit.movePref(vec.set(target).sub(unit).limit(unit.speed()));
            }
        }else if(move){
            pathfind(Pathfinder.fieldCore);
        }

        if(unit.type.canBoost && unit.elevation > 0.001f && !unit.onSolid()){
            unit.elevation = Mathf.approachDelta(unit.elevation, 0f, unit.type.riseSpeed);
        }

        faceTarget();
    }
}
