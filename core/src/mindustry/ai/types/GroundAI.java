package mindustry.ai.types;

import arc.util.*;
import mindustry.ai.Pathfinder.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.pathfinder;

public class GroundAI extends AIController{

    @Override
    public void update(){

        //TODO test
        if(true){
            unit.controlWeapons(true, true);
            unit.aimLook(unit.x(), unit.y() + 10);

            return;
        }

        if(Units.invalidateTarget(target, unit.team(), unit.x(), unit.y(), Float.MAX_VALUE)){
            target = null;

            //TODO this is hacky, cleanup
            if(unit instanceof Legsc){
                unit.lookAt(((Legsc)unit).baseRotation());
            }
        }

        if(retarget()){
            targetClosest();
        }

        Tilec core = unit.closestEnemyCore();

        if(core == null) return;

        float dst = unit.dst(core);

        if(dst < unit.range() / 1.1f){
            target = core;
        }

        if(dst > unit.range() * 0.5f){
            moveToCore(PathTarget.enemyCores);
        }

        boolean rotate = false, shoot = false;

        if(!Units.invalidateTarget(target, unit, unit.range())){
            rotate = true;
            shoot = unit.within(target, unit.range());

            if(unit.type().hasWeapons()){
                unit.aimLook(Predict.intercept(unit, target, unit.type().weapons.first().bullet.speed));
            }
        }

        unit.controlWeapons(rotate, shoot);
    }

    protected void moveToCore(PathTarget path){
        Tile tile = unit.tileOn();
        if(tile == null) return;
        Tile targetTile = pathfinder.getTargetTile(tile, unit.team(), path);

        if(tile == targetTile) return;

        unit.moveAt(vec.trns(unit.angleTo(targetTile), unit.type().speed * Time.delta()));
    }

    protected void moveAwayFromCore(){
        Team enemy = null;
        for(Team team : unit.team().enemies()){
            if(team.active()){
                enemy = team;
                break;
            }
        }

        if(enemy == null){
            for(Team team : unit.team().enemies()){
                enemy = team;
                break;
            }
        }

        if(enemy == null) return;

        Tile tile = unit.tileOn();
        if(tile == null) return;
        Tile targetTile = pathfinder.getTargetTile(tile, enemy, PathTarget.enemyCores);
        Tilec core = unit.closestCore();

        if(tile == targetTile || core == null || unit.within(core, 120f)) return;

        unit.moveAt(vec.trns(unit.angleTo(targetTile), unit.type().speed * Time.delta()));
    }
}
