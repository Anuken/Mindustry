package io.anuke.mindustry.entities.units;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Predict;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.type.Weapon;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Translator;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.world;

public abstract class GroundUnit extends BaseUnit{
    protected static Translator vec = new Translator();

    protected float walkTime;
    protected float baseRotation;
    protected Weapon weapon;

    public final UnitState

    attack = new UnitState(){
        public void entered(){
            target = null;
        }

        public void update(){
            TileEntity core = getClosestEnemyCore();
            float dst = core == null ? 0 : distanceTo(core);

            if(core != null && dst < getWeapon().getAmmo().getRange() / 1.1f){
                target = core;
            }else{
                retarget(() -> targetClosest());
            }

            if(target != null){
                if(core != null){
                    if(dst > getWeapon().getAmmo().getRange() * 0.5f){
                        moveToCore();
                    }

                }else{
                    moveToCore();
                }

                if(distanceTo(target) < getWeapon().getAmmo().getRange()){
                    rotate(angleTo(target));

                    if(Mathf.angNear(angleTo(target), rotation, 13f)){
                        AmmoType ammo = getWeapon().getAmmo();

                        Vector2 to = Predict.intercept(GroundUnit.this, target, ammo.bullet.speed);

                        getWeapon().update(GroundUnit.this, to.x, to.y);
                    }
                }

            }else{
                moveToCore();
            }
        }
    },
    retreat = new UnitState(){
        public void entered(){
            target = null;
        }

        public void update(){
            if(health >= maxHealth() && !isCommanded()){
                state.set(attack);
            }

            moveAwayFromCore();
        }
    };

    @Override
    public void onCommand(UnitCommand command){
        state.set(command == UnitCommand.retreat ? retreat :
                (command == UnitCommand.attack ? attack :
                (null)));
    }

    @Override
    public void init(UnitType type, Team team){
        super.init(type, team);
        this.weapon = type.weapon;
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
        if(Mathf.dst(x, y) > 0.01f){
            baseRotation = Mathf.slerpDelta(baseRotation, Mathf.atan2(x, y), type.baseRotateSpeed);
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

        if(!velocity.isZero(0.0001f) && (target == null || (distanceTo(target) > getWeapon().getAmmo().getRange()))){
            rotation = Mathf.slerpDelta(rotation, velocity.angle(), 0.2f);
        }
    }

    @Override
    public Weapon getWeapon(){
        return weapon;
    }

    public void setWeapon(Weapon weapon){
        this.weapon = weapon;
    }

    @Override
    public void draw(){
        Draw.alpha(hitTime / hitDuration);

        float walktime = walkTime;

        float ft = Mathf.sin(walktime, 6f, 2f);

        Floor floor = getFloorOn();

        if(floor.isLiquid){
            Draw.tint(Color.WHITE, floor.liquidColor, 0.5f);
        }

        for(int i : Mathf.signs){
            Draw.rect(type.legRegion,
                    x + Angles.trnsx(baseRotation, ft * i),
                    y + Angles.trnsy(baseRotation, ft * i),
                    12f * i, 12f - Mathf.clamp(ft * i, 0, 2), baseRotation - 90);
        }

        if(floor.isLiquid){
            Draw.tint(Color.WHITE, floor.liquidColor, drownTime * 0.4f);
        }else{
            Draw.tint(Color.WHITE);
        }

        Draw.rect(type.baseRegion, x, y, baseRotation - 90);

        Draw.rect(type.region, x, y, rotation - 90);

        for(int i : Mathf.signs){
            float tra = rotation - 90, trY = -weapon.getRecoil(this, i > 0) + type.weaponOffsetY;
            float w = i > 0 ? -12 : 12;
            Draw.rect(weapon.equipRegion,
                    x + Angles.trnsx(tra, type.weaponOffsetX * i, trY),
                    y + Angles.trnsy(tra, type.weaponOffsetX * i, trY), w, 12, rotation - 90);
        }

        drawItems();

        Draw.alpha(1f);
    }

    @Override
    public void behavior(){
        if(health <= health * type.retreatPercent && !isCommanded()){
            setState(retreat);
        }
    }

    @Override
    public void updateTargeting(){
        super.updateTargeting();

        if(Units.invalidateTarget(target, team, x, y, Float.MAX_VALUE)){
            target = null;
        }
    }

    @Override
    public void write(DataOutput data) throws IOException{
        super.write(data);
        data.writeByte(weapon.id);
    }

    @Override
    public void read(DataInput data, long time) throws IOException{
        super.read(data, time);
        weapon = Weapon.getByID(data.readByte());
    }

    @Override
    public void writeSave(DataOutput stream) throws IOException{
        stream.writeByte(weapon.id);
        super.writeSave(stream);
    }

    @Override
    public void readSave(DataInput stream) throws IOException{
        weapon = Weapon.getByID(stream.readByte());
        super.readSave(stream);
    }

    protected void moveToCore(){
        Tile tile = world.tileWorld(x, y);
        if(tile == null) return;
        Tile targetTile = world.pathfinder().getTargetTile(team, tile);

        if(tile == targetTile) return;

        vec.trns(baseRotation, type.speed);

        baseRotation = Mathf.slerpDelta(baseRotation, angleTo(targetTile), 0.05f);
        walkTime += Timers.delta();
        velocity.add(vec);
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
        Tile targetTile = world.pathfinder().getTargetTile(enemy, tile);
        TileEntity core = getClosestCore();

        if(tile == targetTile || core == null || distanceTo(core) < 90f) return;

        vec.trns(baseRotation, type.speed);

        baseRotation = Mathf.slerpDelta(baseRotation, angleTo(targetTile), 0.05f);
        walkTime += Timers.delta();
        velocity.add(vec);
    }
}
