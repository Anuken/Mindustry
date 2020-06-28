package mindustry.ai.types;

import mindustry.ai.Pathfinder.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.pathfinder;

public class GroundAI extends AIController{

    @Override
    public void updateUnit(){

        if(Units.invalidateTarget(target, unit.team(), unit.x(), unit.y(), Float.MAX_VALUE)){
            target = null;
        }

        if(retarget()){
            targetClosest();
        }

        Building core = unit.closestEnemyCore();

        if(core != null){
            if(unit.within(core,unit.range() / 1.1f)){
                target = core;
            }

            if(!unit.within(core, unit.range() * 0.5f)){
                moveToCore(FlagTarget.enemyCores);
            }
        }

        boolean rotate = false, shoot = false;

        if(!Units.invalidateTarget(target, unit, unit.range())){
            rotate = true;
            shoot = unit.within(target, unit.range());

            if(unit.type().hasWeapons()){
                unit.aimLook(Predict.intercept(unit, target, unit.type().weapons.first().bullet.speed));
            }
        }else if(unit.moving()){
            unit.lookAt(unit.vel().angle());
        }

        unit.controlWeapons(rotate, shoot);
    }

    protected void moveToCore(FlagTarget path){
        Tile tile = unit.tileOn();
        if(tile == null) return;
        Tile targetTile = pathfinder.getTargetTile(tile, unit.team(), path);

        if(tile == targetTile) return;

        unit.moveAt(vec.trns(unit.angleTo(targetTile), unit.type().speed));
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
        Tile targetTile = pathfinder.getTargetTile(tile, enemy, FlagTarget.enemyCores);
        Building core = unit.closestCore();

        if(tile == targetTile || core == null || unit.within(core, 120f)) return;

        unit.moveAt(vec.trns(unit.angleTo(targetTile), unit.type().speed));
    }
}
