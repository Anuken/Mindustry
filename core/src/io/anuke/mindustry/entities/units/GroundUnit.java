package io.anuke.mindustry.entities.units;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Predict;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.type.Upgrade;
import io.anuke.mindustry.type.Weapon;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.mindustry.world.meta.BlockFlag;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Translator;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.world;

public abstract class GroundUnit extends BaseUnit {
    protected static Translator vec = new Translator();

    protected float walkTime;
    protected float baseRotation;
    protected Weapon weapon;

    @Override
    public void init(UnitType type, Team team) {
        super.init(type, team);
        this.weapon = type.weapon;
    }

    @Override
    public void interpolate() {
        super.interpolate();

        if(interpolator.values.length > 1) {
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
    public UnitState getStartState() {
        return resupply;
    }

    @Override
    public void update() {
        super.update();

        if(!velocity.isZero(0.0001f) && (target == null || !inventory.hasAmmo() || (inventory.hasAmmo() && distanceTo(target) > inventory.getAmmoRange()))){
            rotation = Mathf.slerpDelta(rotation, velocity.angle(), 0.2f);
        }
    }

    @Override
    public Weapon getWeapon() {
        return weapon;
    }

    @Override
    public void draw() {
        Draw.alpha(hitTime / hitDuration);

        float walktime = walkTime;

        float ft = Mathf.sin(walktime, 6f, 2f);

        Floor floor = getFloorOn();

        if(floor.isLiquid){
            Draw.tint(Color.WHITE, floor.liquidColor, 0.5f);
        }

        for (int i : Mathf.signs) {
            Draw.rect(type.legRegion,
                    x + Angles.trnsx(baseRotation, ft * i),
                    y + Angles.trnsy(baseRotation, ft * i),
                    12f * i, 12f - Mathf.clamp(ft * i, 0, 2), baseRotation - 90);
        }

        if(floor.isLiquid) {
            Draw.tint(Color.WHITE, floor.liquidColor, drownTime * 0.4f);
        }else {
            Draw.tint(Color.WHITE);
        }

        Draw.rect(type.baseRegion, x, y, baseRotation- 90);

        Draw.rect(type.region, x, y, rotation -90);

        for (int i : Mathf.signs) {
            float tra = rotation - 90, trY = - weapon.getRecoil(this, i > 0) + type.weaponOffsetY;
            float w = i > 0 ? -12 : 12;
            Draw.rect(weapon.equipRegion,
                    x + Angles.trnsx(tra, type.weaponOffsetX * i, trY),
                    y + Angles.trnsy(tra, type.weaponOffsetX * i, trY), w, 12, rotation - 90);
        }

        drawItems();

        Draw.alpha(1f);
    }

    @Override
    public void behavior() {
        if(health <= health * type.retreatPercent && !isWave){
            setState(retreat);
        }
    }

    @Override
    public void updateTargeting() {
        super.updateTargeting();

        if(Units.invalidateTarget(target, team,  x, y, Float.MAX_VALUE)){
            target = null;
        }
    }

    @Override
    public void write(DataOutput data) throws IOException {
        super.write(data);
        data.writeByte(weapon.id);
    }

    @Override
    public void read(DataInput data, long time) throws IOException {
        super.read(data, time);
        weapon = Upgrade.getByID(data.readByte());
    }

    @Override
    public void writeSave(DataOutput stream) throws IOException {
        stream.writeByte(weapon.id);
        super.writeSave(stream);
    }

    @Override
    public void readSave(DataInput stream) throws IOException{
        weapon = Upgrade.getByID(stream.readByte());
        super.readSave(stream);
    }

    public void setWeapon(Weapon weapon){
        this.weapon = weapon;
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
        Tile tile = world.tileWorld(x, y);
        Tile targetTile = world.pathfinder().getTargetTile(Vars.state.teams.enemiesOf(team).first(), tile);

        if(tile == targetTile) return;

        vec.trns(baseRotation, type.speed);

        baseRotation = Mathf.slerpDelta(baseRotation, angleTo(targetTile), 0.05f);
        walkTime += Timers.delta();
        velocity.add(vec);
    }

    public final UnitState

    resupply = new UnitState(){
        public void entered() {
            target = null;
        }

        public void update() {
            Tile tile = Geometry.findClosest(x, y, world.indexer().getAllied(team, BlockFlag.resupplyPoint));

            if (tile != null && distanceTo(tile) > 40) {
                moveAwayFromCore();
            }

            //TODO move toward resupply point
            if(isWave || inventory.totalAmmo() + 10 >= inventory.ammoCapacity()){
                state.set(attack);
            }
        }
    },
    attack = new UnitState(){
        public void entered() {
            target = null;
        }

        public void update() {
            TileEntity core = getClosestEnemyCore();
            float dst = core == null ? 0 :distanceTo(core);

            if(core != null && inventory.hasAmmo() && dst < inventory.getAmmo().getRange()){
                target = core;
            }else {
                retarget(() -> targetClosest());
            }

            if(!inventory.hasAmmo()) {
                state.set(resupply);
            }else if(target != null){
                if(core != null){
                    if(dst > inventory.getAmmo().getRange() * 0.5f){
                        moveToCore();
                    }

                }else{
                    moveToCore();
                }

                if(distanceTo(target) < inventory.getAmmo().getRange()){
                    rotate(angleTo(target));

                    if (Mathf.angNear(angleTo(target), rotation, 13f)) {
                        AmmoType ammo = inventory.getAmmo();

                        Vector2 to = Predict.intercept(GroundUnit.this, target, ammo.bullet.speed);

                        getWeapon().update(GroundUnit.this, to.x, to.y);
                    }
                }

            }else{
                moveToCore();
            }
        }
    },
    retreat = new UnitState() {
        public void entered() {
            target = null;
        }

        public void update() {
            if(health >= health){
                state.set(attack);
            }

            moveAwayFromCore();
        }
    };
}
