package io.anuke.mindustry.entities.units.types;

import io.anuke.mindustry.content.AmmoTypes;
import io.anuke.mindustry.content.fx.UnitFx;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.FlyingUnitType;
import io.anuke.mindustry.entities.units.UnitState;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.resource.AmmoType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.flags.BlockFlag;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.world;

public class Vtol extends FlyingUnitType {
    private float retreatHealth = 10f;

    public Vtol(){
        super("vtol");
        setAmmo(AmmoTypes.basicIron);
        speed = 0.3f;
        maxVelocity = 2f;
    }

    @Override
    public void drawUnder(BaseUnit unit) {
        float rotation = unit.rotation - 90;
        float scl = 0.6f + Mathf.absin(Timers.time(), 1f, 0.3f);
        float dy = -6f*scl;

        Draw.color(Palette.lighterOrange, Palette.lightFlame, Mathf.absin(Timers.time(), 3f, 0.7f));

        Draw.rect("vtol-flame",
                unit.x + Angles.trnsx(rotation, 0, dy),
                unit.y + Angles.trnsy(rotation, 0, dy), Mathf.atan2(0, dy) + rotation);

        Draw.color();


        /*
        for(int i : Mathf.signs) {

            float rotation = unit.rotation - 90;
            float scl = 0.7f + Mathf.absin(Timers.time(), 3f, 0.2f);
            float dx = 5f * i*scl, dx2 = 6f * i*scl;
            float dy = 4f*scl, dy2 = -5f*scl;

            Draw.color(Palette.lighterOrange, Palette.lightFlame, Mathf.absin(Timers.time(), 3f, 0.7f));

            Draw.rect("vtol-flame",
                    unit.x + Angles.trnsx(rotation, dx, dy),
                    unit.y + Angles.trnsy(rotation, dx, dy), Mathf.atan2(dx, dy) + rotation);

            Draw.rect("vtol-flame",
                    unit.x + Angles.trnsx(rotation, dx2, dy2),
                    unit.y + Angles.trnsy(rotation, dx2, dy2), Mathf.atan2(dx2, dy2) + rotation);

            Draw.color();
        }*/
    }

    @Override
    public void draw(BaseUnit unit) {
        Draw.alpha(unit.hitTime / Unit.hitDuration);

        Draw.rect(name, unit.x, unit.y, unit.rotation - 90);
        for(int i : Mathf.signs){
            Draw.rect(name + "-booster-1", unit.x, unit.y, 12*i, 12, unit.rotation - 90);
            Draw.rect(name + "-booster-2", unit.x, unit.y, 12*i, 12, unit.rotation - 90);
        }

        Draw.alpha(1f);
    }

    @Override
    public void update(BaseUnit unit) {
        super.update(unit);

        unit.x += Mathf.sin(Timers.time() + unit.id * 999, 25f, 0.07f);
        unit.y += Mathf.cos(Timers.time() + unit.id * 999, 25f, 0.07f);

        if(unit.velocity.len() <= 0.2f){
            unit.rotation += Mathf.sin(Timers.time() + unit.id * 99, 10f, 8f);
        }

        if(unit.timer.get(timerBoost, 2)){
            unit.effectAt(UnitFx.vtolHover, unit.rotation + 180f, 4f, 0);
        }
    }

    @Override
    public void behavior(BaseUnit unit) {
        if(unit.health <= retreatHealth &&
                Geometry.findClosest(unit.x, unit.y, world.indexer().getAllied(unit.team, BlockFlag.repair)) != null){
            unit.setState(retreat);
        }
    }

    @Override
    public UnitState getStartState(){
        return resupply;
    }

    void circle(BaseUnit unit, float circleLength){
        vec.set(unit.target.x - unit.x, unit.target.y - unit.y);

        if(vec.len() < circleLength){
            vec.rotate((circleLength-vec.len())/circleLength * 180f);
        }

        vec.setLength(speed * Timers.delta());

        unit.velocity.add(vec);
    }

    void attack(BaseUnit unit, float circleLength){
        vec.set(unit.target.x - unit.x, unit.target.y - unit.y);

        float ang = unit.angleTo(unit.target);
        float diff = Angles.angleDist(ang, unit.rotation);

        if(diff > 100f && vec.len() < circleLength){
            vec.setAngle(unit.velocity.angle());
        }else{
            vec.setAngle(Mathf.slerpDelta(unit.velocity.angle(), vec.angle(),  0.44f));
        }

        vec.setLength(speed*Timers.delta());

        unit.velocity.add(vec);
    }

    public final UnitState

    resupply = new UnitState(){
        public void entered(BaseUnit unit) {
            unit.target = null;
        }

        public void update(BaseUnit unit) {
            if(unit.inventory.totalAmmo() + 10 >= unit.inventory.ammoCapacity()){
                unit.state.set(unit, attack);
            }else if(!unit.targetHasFlag(BlockFlag.resupplyPoint)){
                if(unit.timer.get(timerTarget, 20)) {
                    Tile target = Geometry.findClosest(unit.x, unit.y, world.indexer().getAllied(unit.team, BlockFlag.resupplyPoint));
                    if (target != null) unit.target = target.entity;
                }
            }else{
                circle(unit, 20f);
            }
        }
    },
    attack = new UnitState(){
        public void entered(BaseUnit unit) {
            unit.target = null;
        }

        public void update(BaseUnit unit) {
            if(!unit.inventory.hasAmmo()) {
                unit.state.set(unit, resupply);
            }else if (unit.target == null){
                if(unit.timer.get(timerTarget, 20)) {
                    Unit closest = Units.getClosestEnemy(unit.team, unit.x, unit.y,
                            unit.inventory.getAmmo().getRange(), other -> other.distanceTo(unit) < 60f);
                    if(closest != null){
                        unit.target = closest;
                    }else {
                        Tile target = Geometry.findClosest(unit.x, unit.y, world.indexer().getEnemy(unit.team, BlockFlag.resupplyPoint));
                        if (target != null) unit.target = target.entity;
                    }
                }
            }else{
                attack(unit, 150f);

                if (unit.timer.get(timerReload, 7) && Mathf.angNear(unit.angleTo(unit.target), unit.rotation, 13f)
                        && unit.distanceTo(unit.target) < unit.inventory.getAmmo().getRange()) {
                    AmmoType ammo = unit.inventory.getAmmo();
                    unit.inventory.useAmmo();

                    shoot(unit, ammo, unit.rotation, 4f);
                }
            }
        }
    },
    retreat = new UnitState() {
        public void entered(BaseUnit unit) {
            unit.target = null;
        }

        public void update(BaseUnit unit) {
            if(unit.health >= health){
                unit.state.set(unit, attack);
            }else if(!unit.targetHasFlag(BlockFlag.repair)){
                if(unit.timer.get(timerTarget, 20)) {
                    Tile target = Geometry.findClosest(unit.x, unit.y, world.indexer().getAllied(unit.team, BlockFlag.repair));
                    if (target != null) unit.target = target.entity;
                }
            }else{
                circle(unit, 20f);
            }
        }
    };

}
