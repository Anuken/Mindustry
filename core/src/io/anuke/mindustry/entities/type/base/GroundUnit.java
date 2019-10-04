package io.anuke.mindustry.entities.type.base;

import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.ai.Pathfinder.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.bullet.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.entities.units.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.mindustry.world.meta.*;

import static io.anuke.mindustry.Vars.*;

public class GroundUnit extends BaseUnit{
    protected static Vector2 vec = new Vector2();

    protected float walkTime;
    protected float stuckTime;
    protected float baseRotation;

    public final UnitState

    attack = new UnitState(){
        public void entered(){
            target = null;
        }

        public void update(){
            TileEntity core = getClosestEnemyCore();

            if(core == null){
                Tile closestSpawn = getClosestSpawner();
                if(closestSpawn == null || !withinDst(closestSpawn, Vars.state.rules.dropZoneRadius + 85f)){
                    moveToCore(PathTarget.enemyCores);
                }
            }else{

                float dst = dst(core);

                if(dst < getWeapon().bullet.range() / 1.1f){
                    target = core;
                }

                if(dst > getWeapon().bullet.range() * 0.5f){
                    moveToCore(PathTarget.enemyCores);
                }
            }
        }
    },
    rally = new UnitState(){
        public void update(){
            Tile target = getClosest(BlockFlag.rally);

            if(target != null && dst(target) > 80f){
                moveToCore(PathTarget.rallyPoints);
            }
        }
    },
    retreat = new UnitState(){
        public void entered(){
            target = null;
        }

        public void update(){
            moveAwayFromCore();
        }
    };

    @Override
    public void onCommand(UnitCommand command){
        state.set(command == UnitCommand.retreat ? retreat :
        command == UnitCommand.attack ? attack :
        command == UnitCommand.rally ? rally :
        null);
    }

    @Override
    public void interpolate(){
        super.interpolate();

        if(interpolator.values.length > 1){
            baseRotation = interpolator.values[1];
        }
    }

    @Override
    public void move(float x, float y){
        float dst = Mathf.dst(x, y);
        if(dst > 0.01f){
            baseRotation = Mathf.slerp(baseRotation, Mathf.angle(x, y), type.baseRotateSpeed * (dst / type.speed));
        }
        super.move(x, y);
    }

    @Override
    public UnitState getStartState(){
        return attack;
    }

    @Override
    public void update(){
        super.update();

        stuckTime = !vec.set(x, y).sub(lastPosition()).isZero(0.0001f) ? 0f : stuckTime + Time.delta();

        if(!velocity.isZero()){
            baseRotation = Mathf.slerpDelta(baseRotation, velocity.angle(), 0.05f);
        }

        if(stuckTime < 1f){
            walkTime += Time.delta();
        }
    }

    @Override
    public Weapon getWeapon(){
        return type.weapon;
    }

    @Override
    public void draw(){
        Draw.mixcol(Color.white, hitTime / hitDuration);

        float ft = Mathf.sin(walkTime * type.speed * 5f, 6f, 2f + type.hitsize / 15f);

        Floor floor = getFloorOn();

        if(floor.isLiquid){
            Draw.color(Color.white, floor.color, 0.5f);
        }

        for(int i : Mathf.signs){
            Draw.rect(type.legRegion,
            x + Angles.trnsx(baseRotation, ft * i),
            y + Angles.trnsy(baseRotation, ft * i),
            type.legRegion.getWidth() * i * Draw.scl, type.legRegion.getHeight() * Draw.scl - Mathf.clamp(ft * i, 0, 2), baseRotation - 90);
        }

        if(floor.isLiquid){
            Draw.color(Color.white, floor.color, drownTime * 0.4f);
        }else{
            Draw.color(Color.white);
        }

        Draw.rect(type.baseRegion, x, y, baseRotation - 90);

        Draw.rect(type.region, x, y, rotation - 90);

        for(int i : Mathf.signs){
            float tra = rotation - 90, trY = -type.weapon.getRecoil(this, i > 0) + type.weaponOffsetY;
            float w = -i * type.weapon.region.getWidth() * Draw.scl;
            Draw.rect(type.weapon.region,
            x + Angles.trnsx(tra, getWeapon().width * i, trY),
            y + Angles.trnsy(tra, getWeapon().width * i, trY), w, type.weapon.region.getHeight() * Draw.scl, rotation - 90);
        }

        Draw.mixcol();
    }

    @Override
    public void behavior(){

        if(!Units.invalidateTarget(target, this)){
            if(dst(target) < getWeapon().bullet.range()){
                rotate(angleTo(target));

                if(Angles.near(angleTo(target), rotation, 13f)){
                    BulletType ammo = getWeapon().bullet;

                    Vector2 to = Predict.intercept(GroundUnit.this, target, ammo.speed);

                    getWeapon().update(GroundUnit.this, to.x, to.y);
                }
            }
        }
    }

    @Override
    public void updateTargeting(){
        super.updateTargeting();

        if(Units.invalidateTarget(target, team, x, y, Float.MAX_VALUE)){
            target = null;
        }

        if(retarget()){
            targetClosest();
        }
    }

    protected void patrol(){
        vec.trns(baseRotation, type.speed * Time.delta());
        velocity.add(vec.x, vec.y);
        vec.trns(baseRotation, type.hitsizeTile * 5);
        Tile tile = world.tileWorld(x + vec.x, y + vec.y);
        if((tile == null || tile.solid() || tile.floor().drownTime > 0 || tile.floor().isLiquid) || stuckTime > 10f){
            baseRotation += Mathf.sign(id % 2 - 0.5f) * Time.delta() * 3f;
        }

        rotation = Mathf.slerpDelta(rotation, velocity.angle(), type.rotatespeed);
    }

    protected void circle(float circleLength){
        if(target == null) return;

        vec.set(target.getX() - x, target.getY() - y);

        if(vec.len() < circleLength){
            vec.rotate((circleLength - vec.len()) / circleLength * 180f);
        }

        vec.setLength(type.speed * Time.delta());

        velocity.add(vec);
    }

    protected void moveToCore(PathTarget path){
        Tile tile = world.tileWorld(x, y);
        if(tile == null) return;
        Tile targetTile = pathfinder.getTargetTile(tile, team, path);

        if(tile == targetTile) return;

        velocity.add(vec.trns(angleTo(targetTile), type.speed * Time.delta()));
        if(Units.invalidateTarget(target, this)){
            rotation = Mathf.slerpDelta(rotation, baseRotation, type.rotatespeed);
        }
    }

    protected void moveAwayFromCore(){
        Team enemy = null;
        for(Team team : Vars.state.teams.enemiesOf(team)){
            if(Vars.state.teams.isActive(team)){
                enemy = team;
                break;
            }
        }

        if(enemy == null){
            for(Team team : Vars.state.teams.enemiesOf(team)){
                enemy = team;
                break;
            }
        }

        if(enemy == null) return;

        Tile tile = world.tileWorld(x, y);
        if(tile == null) return;
        Tile targetTile = pathfinder.getTargetTile(tile, enemy, PathTarget.enemyCores);
        TileEntity core = getClosestCore();

        if(tile == targetTile || core == null || dst(core) < 120f) return;

        velocity.add(vec.trns(angleTo(targetTile), type.speed * Time.delta()));
        rotation = Mathf.slerpDelta(rotation, baseRotation, type.rotatespeed);
    }
}
