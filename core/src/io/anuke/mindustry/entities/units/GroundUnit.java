package io.anuke.mindustry.entities.units;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Predict;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.mindustry.world.meta.BlockFlag;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Translator;

import static io.anuke.mindustry.Vars.world;

public abstract class GroundUnit extends BaseUnit {
    protected static Translator vec = new Translator();
    protected static float maxAim = 30f;

    protected float walkTime;
    protected float baseRotation;

    public GroundUnit(UnitType type, Team team) {
        super(type, team);
    }

    public GroundUnit(){

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
        baseRotation = Mathf.slerpDelta(baseRotation, Mathf.atan2(x, y), type.baseRotateSpeed);
        super.move(x, y);
    }

    @Override
    public UnitState getStartState() {
        return resupply;
    }

    @Override
    public void update() {
        super.update();

        if(!velocity.isZero(0.001f) && (target == null || !inventory.hasAmmo() || (inventory.hasAmmo() && distanceTo(target) > inventory.getAmmoRange()))){
            rotation = Mathf.slerpDelta(rotation, velocity.angle(), 0.2f);
        }
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
            Draw.rect(type.name + "-leg",
                    x + Angles.trnsx(baseRotation, ft * i),
                    y + Angles.trnsy(baseRotation, ft * i),
                    12f * i, 12f - Mathf.clamp(ft * i, 0, 2), baseRotation - 90);
        }

        if(floor.isLiquid) {
            Draw.tint(Color.WHITE, floor.liquidColor, drownTime * 0.4f);
        }else {
            Draw.tint(Color.WHITE);
        }

        Draw.rect(type.name + "-base", x, y, baseRotation- 90);

        Draw.rect(type.name, x, y, rotation -90);

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
                    if(dst > inventory.getAmmo().getRange() * 0.7f){
                        moveToCore();
                    }

                    rotate(angleTo(target));

                }else{
                    moveToCore();
                    rotate(angleTo(target));
                }

                if (timer.get(timerReload, type.reload) && Mathf.angNear(angleTo(target), rotation, 13f)
                        && distanceTo(target) < inventory.getAmmo().getRange()) {
                    AmmoType ammo = inventory.getAmmo();
                    inventory.useAmmo();
                    rotate(angleTo(target));

                    Vector2 to = Predict.intercept(GroundUnit.this, target, ammo.bullet.speed);

                    shoot(ammo, Angles.moveToward(rotation, angleTo(to.x, to.y), maxAim));
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
