package io.anuke.mindustry.entities.type;

import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.math.Angles;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Predict;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.units.UnitState;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.type.Weapon;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;

import static io.anuke.mindustry.Vars.world;

public abstract class GroundUnit extends BaseUnit{
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
                setState(patrol);
                return;
            }

            float dst = dst(core);

            if(dst < getWeapon().bullet.range() / 1.1f){
                target = core;
            }

            if(dst > getWeapon().bullet.range() * 0.5f){
                moveToCore();
            }
        }
    },
    patrol = new UnitState(){
        public void update(){
            TileEntity target = getClosestCore();

            if(target != null){
                if(dst(target) > 400f){
                    moveAwayFromCore();
                }else if(!(!Units.invalidateTarget(GroundUnit.this.target, GroundUnit.this) && dst(GroundUnit.this.target) < getWeapon().bullet.range())){
                    patrol();
                }
            }
        }
    };

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
        Draw.mixcol(Color.WHITE, hitTime / hitDuration);

        float ft = Mathf.sin(walkTime * type.speed * 5f, 6f, 2f + type.hitsize / 15f);

        Floor floor = getFloorOn();

        if(floor.isLiquid){
            Draw.color(Color.WHITE, floor.color, 0.5f);
        }

        for(int i : Mathf.signs){
            Draw.rect(type.legRegion,
            x + Angles.trnsx(baseRotation, ft * i),
            y + Angles.trnsy(baseRotation, ft * i),
            type.legRegion.getWidth() * i * Draw.scl, type.legRegion.getHeight() * Draw.scl - Mathf.clamp(ft * i, 0, 2), baseRotation - 90);
        }

        if(floor.isLiquid){
            Draw.color(Color.WHITE, floor.color, drownTime * 0.4f);
        }else{
            Draw.color(Color.WHITE);
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

        drawItems();

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
        vec.trns(baseRotation, type.hitsizeTile * 3);
        Tile tile = world.tileWorld(x + vec.x, y + vec.y);
        if((tile == null || tile.solid() || tile.floor().drownTime > 0) || stuckTime > 10f){
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

    protected void moveToCore(){
        Tile tile = world.tileWorld(x, y);
        if(tile == null) return;
        Tile targetTile = world.pathfinder.getTargetTile(team, tile);

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

        if(enemy == null) return;

        Tile tile = world.tileWorld(x, y);
        if(tile == null) return;
        Tile targetTile = world.pathfinder.getTargetTile(enemy, tile);
        TileEntity core = getClosestCore();

        if(tile == targetTile || core == null || dst(core) < 90f) return;

        velocity.add(vec.trns(angleTo(targetTile), type.speed * Time.delta()));
        rotation = Mathf.slerpDelta(rotation, baseRotation, type.rotatespeed);
    }
}
