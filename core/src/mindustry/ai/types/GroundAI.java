package mindustry.ai.types;

import arc.math.*;
import mindustry.ai.*;
import mindustry.ai.Pathfinder.Flowfield;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class GroundAI extends AIController{
    private Flowfield path = null;

    @Override
    public void init() {
        path = findPath(Pathfinder.fieldCore);
    }

    @Override
    public void updateMovement(){
        Building core = unit.closestEnemyCore();

        if(core == null || !unit.within(core, unit.type.range * 0.1f)){
            boolean move = true;

            if(state.rules.waves && unit.team == state.rules.defaultTeam){
                Tile spawner = getClosestSpawner();
                if(spawner != null && unit.within(spawner, state.rules.dropZoneRadius + 120f)) move = false;
            }

            if(move){
                if(unit.team == state.rules.waveTeam) {
                    if(path != null){
                        moveWithField(path);
                    }else{
                        path = findPath(Pathfinder.fieldCore);
                    }
                }else{
                    if(target != null && target.within(unit, unit.type.range * 5f)) {
                        moveTo(target, unit.type.range * 0.2f);
                        unit.lookAt(target);
                    }else{
                        pathfind(Pathfinder.fieldCore);
                    }
                }
            }
        }

        if(unit.type.canBoost && unit.elevation > 0.001f && !unit.onSolid()){
            unit.elevation = Mathf.approachDelta(unit.elevation, 0f, unit.type.riseSpeed);
        }

        if((unit.type.omniMovement || unit instanceof Mechc) && unit.moving()){
            unit.lookAt(unit.vel().angle());
        }
    }
}
