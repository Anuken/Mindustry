package io.anuke.mindustry.entities.units;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.world.BlockFlag;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.Floor;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Translator;

import static io.anuke.mindustry.Vars.world;

public abstract class GroundUnitType extends BaseUnit {
    protected static Translator vec = new Translator();
    protected static float maxAim = 30f;

    protected float walkTime;

    public GroundUnitType(UnitType type, Team team) {
        super(type, team);
    }

    @Override
    public UnitState getStartState() {
        return resupply;
    }

    @Override
    public void update() {
        super.update();

        if(!velocity.isZero(0.0001f) && (target == null
                || (inventory.hasAmmo() && distanceTo(target) > inventory.getAmmo().getRange()))){
            rotation = velocity.angle();
        }
    }

    @Override
    public void drawSmooth() {
        Draw.alpha(hitTime / hitDuration);

        float walktime = walkTime;

        float ft = Mathf.sin(walktime, 6f, 2f);

        Floor floor = getFloorOn();

        if(floor.liquid){
            Draw.tint(Hue.mix(Color.WHITE, floor.liquidColor, 0.5f));
        }

        for (int i : Mathf.signs) {
            Draw.rect(type.name + "-leg",
                    x + Angles.trnsx(baseRotation, ft * i),
                    y + Angles.trnsy(baseRotation, ft * i),
                    12f * i, 12f - Mathf.clamp(ft * i, 0, 2), baseRotation - 90);
        }

        if(floor.liquid) {
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
        if(health <= health * type.retreatPercent && !inventory.isInfiniteAmmo()){
            setState(retreat);
        }
    }

    @Override
    public void updateTargeting() {
        super.updateTargeting();

        if(Units.invalidateTarget(target, this)){
            target = null;
        }
    }

    protected void moveToCore(){
        Tile tile = world.tileWorld(x, y);
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
            if(inventory.totalAmmo() + 10 >= inventory.ammoCapacity()){
                state.set(attack);
            }
        }
    },
    attack = new UnitState(){
        public void entered() {
            target = null;
        }

        public void update() {
            retarget(() -> {
                Unit closest = Units.getClosestEnemy(team, x, y, inventory.getAmmo().getRange(), other -> true);
                if(closest != null){
                    target = closest;
                }else {
                    Tile target = Geometry.findClosest(x, y, world.indexer().getEnemy(team, BlockFlag.target));
                    if (target != null) GroundUnitType.this.target = target.entity;
                }
            });

            if(!inventory.hasAmmo()) {
                state.set(resupply);
            }else{
                if(distanceTo(target) > inventory.getAmmo().getRange() * 0.7f){
                    moveToCore();
                }else{
                    rotate(angleTo(target));
                }

                if (timer.get(timerReload, type.reload) && Mathf.angNear(angleTo(target), rotation, 13f)
                        && distanceTo(target) < inventory.getAmmo().getRange()) {
                    AmmoType ammo = inventory.getAmmo();
                    inventory.useAmmo();
                    rotate(angleTo(target));

                    shoot(ammo, Angles.moveToward(rotation, angleTo(target), maxAim), 4f);
                }
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
