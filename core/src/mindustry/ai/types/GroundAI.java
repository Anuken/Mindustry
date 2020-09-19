package mindustry.ai.types;

import arc.math.*;
import mindustry.ai.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import java.util.*;

import static mindustry.Vars.*;

public class GroundAI extends AIController{
    //static final float commandCooldown = 60f * 10;
    //float commandTimer = 60*3;

    @Override
    public void updateMovement(){

        Building core = unit.closestEnemyCore();

        if(core != null && unit.within(core, unit.range() / 1.1f + core.block.size * tilesize / 2f)){
            target = core;
            Arrays.fill(targets, core);
        }

        if((core == null || !unit.within(core, unit.range() * 0.5f)) && command() == UnitCommand.attack){
            boolean move = true;

            if(state.rules.waves && unit.team == state.rules.defaultTeam){
                Tile spawner = getClosestSpawner();
                if(spawner != null && unit.within(spawner, state.rules.dropZoneRadius + 120f)) move = false;
            }

            if(move) moveTo(Pathfinder.fieldCore);
        }

        if(command() == UnitCommand.rally){
            Teamc target = targetFlag(unit.x, unit.y, BlockFlag.rally, false);

            if(target != null && !unit.within(target, 70f)){
                moveTo(Pathfinder.fieldRally);
            }
        }

        if(unit.type().canBoost && !unit.onSolid()){
            unit.elevation = Mathf.approachDelta(unit.elevation, 0f, 0.08f);
        }

        if(!Units.invalidateTarget(target, unit, unit.range()) && unit.type().rotateShooting){
            if(unit.type().hasWeapons()){
                //TODO certain units should not look at the target, e.g. ships
                unit.lookAt(Predict.intercept(unit, target, unit.type().weapons.first().bullet.speed));
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

    protected void moveTo(int pathType){
        int costType =
            unit instanceof Legsc ? Pathfinder.costLegs :
            unit instanceof WaterMovec ? Pathfinder.costWater :
            Pathfinder.costGround;

        Tile tile = unit.tileOn();
        if(tile == null) return;
        Tile targetTile = pathfinder.getTargetTile(tile, pathfinder.getField(unit.team, costType, pathType));

        if(tile == targetTile || (costType == Pathfinder.costWater && !targetTile.floor().isLiquid)) return;

        unit.moveAt(vec.trns(unit.angleTo(targetTile), unit.type().speed));
    }
}
