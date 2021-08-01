package mindustry.ai.types;

import arc.math.geom.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class SuicideAI extends GroundAI{
    static boolean blockedByBlock;

    @Override
    public void updateUnit(){

        if(Units.invalidateTarget(target, unit.team, unit.x, unit.y, Float.MAX_VALUE)){
            target = null;
        }

        if(retarget()){
            target = target(unit.x, unit.y, unit.range(), unit.type.targetAir, unit.type.targetGround);
        }

        Building core = unit.closestEnemyCore();

        boolean rotate = false, shoot = false, moveToTarget = false;

        if(target == null){
            target = core;
        }

        if(!Units.invalidateTarget(target, unit, unit.range()) && unit.hasWeapons()){
            rotate = true;
            shoot = unit.within(target, unit.type.weapons.first().bullet.range() +
                (target instanceof Building b ? b.block.size * Vars.tilesize / 2f : ((Hitboxc)target).hitSize() / 2f));

            if(unit.type.hasWeapons()){
                unit.aimLook(Predict.intercept(unit, target, unit.type.weapons.first().bullet.speed));
            }

            //do not move toward walls or transport blocks
            if(!(target instanceof Building build && !(build.block instanceof CoreBlock) && (
                build.block.group == BlockGroup.walls ||
                build.block.group == BlockGroup.liquids ||
                build.block.group == BlockGroup.transportation
            ))){
                blockedByBlock = false;

                //raycast for target
                boolean blocked = Vars.world.raycast(unit.tileX(), unit.tileY(), target.tileX(), target.tileY(), (x, y) -> {
                    for(Point2 p : Geometry.d4c){
                        Tile tile = Vars.world.tile(x + p.x, y + p.y);
                        if(tile != null && tile.build == target) return false;
                        if(tile != null && tile.build != null && tile.build.team != unit.team()){
                            blockedByBlock = true;
                            return true;
                        }else{
                            return tile == null || tile.solid();
                        }
                    }
                    return false;
                });

                //shoot when there's an enemy block in the way
                if(blockedByBlock){
                    shoot = true;
                }

                if(!blocked){
                    moveToTarget = true;
                    //move towards target directly
                    unit.moveAt(vec.set(target).sub(unit).limit(unit.speed()));
                }
            }
        }

        if(!moveToTarget){
            if(command() == UnitCommand.rally){
                Teamc target = targetFlag(unit.x, unit.y, BlockFlag.rally, false);

                if(target != null && !unit.within(target, 70f)){
                    pathfind(Pathfinder.fieldRally);
                }
            }else if(command() == UnitCommand.attack){
                boolean move = true;

                //stop moving toward the drop zone if applicable
                if(core == null && state.rules.waves && unit.team == state.rules.defaultTeam){
                    Tile spawner = getClosestSpawner();
                    if(spawner != null && unit.within(spawner, state.rules.dropZoneRadius + 120f)){
                        move = false;
                    }
                }

                if(move){
                    pathfind(Pathfinder.fieldCore);
                }
            }

            if(unit.moving()) unit.lookAt(unit.vel().angle());
        }

        unit.controlWeapons(rotate, shoot);
    }

    @Override
    protected Teamc target(float x, float y, float range, boolean air, boolean ground){
        return Units.closestTarget(unit.team, x, y, range, u -> u.checkTarget(air, ground), t -> ground &&
            !(t.block instanceof Conveyor || t.block instanceof Conduit)); //do not target conveyors/conduits
    }
}
