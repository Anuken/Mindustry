package mindustry.ai.types;

import arc.math.*;
import mindustry.ai.Pathfinder.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class GroundAI extends AIController{
    //static final float commandCooldown = 60f * 10;
    //float commandTimer = 60*3;

    @Override
    public void updateMovement(){

        Building core = unit.closestEnemyCore();

        if(core != null && unit.within(core, unit.range() / 1.1f)){
            target = core;
        }

        if((core == null || !unit.within(core, unit.range() * 0.5f)) && command() == UnitCommand.attack){
            boolean move = true;

            if(state.rules.waves && unit.team == state.rules.defaultTeam){
                Tile spawner = getClosestSpawner();
                if(spawner != null && unit.within(spawner, state.rules.dropZoneRadius + 120f)) move = false;
            }

            if(move) moveToCore(FlagTarget.enemyCores);
        }

        if(command() == UnitCommand.rally){
            Teamc target = targetFlag(unit.x, unit.y, BlockFlag.rally, false);

            if(target != null && !unit.within(target, 70f)){
                moveToCore(FlagTarget.rallyPoints);
            }
        }

        if(unit.type().canBoost){
            unit.elevation = Mathf.approachDelta(unit.elevation, 0f, 0.08f);
        }

        if(!Units.invalidateTarget(target, unit, unit.range())){
            if(unit.type().hasWeapons()){
                unit.aimLook(Predict.intercept(unit, target, unit.type().weapons.first().bullet.speed));
            }
        }else if(unit.moving()){
            unit.lookAt(unit.vel().angle());
        }

        //auto-command works but it's very buggy
        /*
        if(unit instanceof Commanderc){
            Commanderc c = (Commanderc)unit;
            //try to command when missing members
            if(c.controlling().size <= unit.type().commandLimit/2){
                commandTimer -= Time.delta;

                if(commandTimer <= 0){
                    c.commandNearby(new SquareFormation(), u -> !(u.controller() instanceof FormationAI) && !(u instanceof Commanderc));
                    commandTimer = commandCooldown;
                }
            }
        }*/
    }

    protected void moveToCore(FlagTarget path){
        Tile tile = unit.tileOn();
        if(tile == null) return;
        Tile targetTile = pathfinder.getTargetTile(tile, unit.team, path);

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
