package mindustry.ai.types;

import mindustry.*;
import mindustry.ai.Pathfinder.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.world.*;

public class SuicideAI extends GroundAI{
    static boolean blockedByBlock;

    @Override
    public void updateUnit(){

        if(Units.invalidateTarget(target, unit.team(), unit.x(), unit.y(), Float.MAX_VALUE)){
            target = null;
        }

        if(retarget()){
            target = target(unit.x, unit.y, unit.range(), unit.type().targetAir, unit.type().targetGround);
        }

        Building core = unit.closestEnemyCore();

        boolean rotate = false, shoot = false;

        if(!Units.invalidateTarget(target, unit, unit.range())){
            rotate = true;
            shoot = unit.within(target, unit.type().weapons.first().bullet.range() +
                (target instanceof Building ? ((Building)target).block().size * Vars.tilesize / 2f : ((Hitboxc)target).hitSize() / 2f));

            if(unit.type().hasWeapons()){
                unit.aimLook(Predict.intercept(unit, target, unit.type().weapons.first().bullet.speed));
            }

            blockedByBlock = false;

            //raycast for target
            boolean blocked = Vars.world.raycast(unit.tileX(), unit.tileY(), target.tileX(), target.tileY(), (x, y) -> {
                Tile tile = Vars.world.tile(x, y);
                if(tile != null && tile.build == target) return false;
                if(tile != null && tile.build != null && tile.build.team() != unit.team()){
                    blockedByBlock = true;
                    return true;
                }else{
                    return tile == null || tile.solid();
                }
            });

            //shoot when there's an enemy block in the way
            if(blockedByBlock){
                shoot = true;
            }

            if(!blocked){
                //move towards target directly
                unit.moveAt(vec.set(target).sub(unit).limit(unit.type().speed));
            }

        }else{
            if(core != null){
                moveToCore(FlagTarget.enemyCores);
            }

            if(unit.moving()) unit.lookAt(unit.vel().angle());
        }

        unit.controlWeapons(rotate, shoot);
    }
}
