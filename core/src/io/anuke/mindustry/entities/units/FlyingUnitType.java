package io.anuke.mindustry.entities.units;

import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.resource.AmmoType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.flags.BlockFlag;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.world;

public class FlyingUnitType extends UnitType {
    protected static Vector2 vec = new Vector2();

    protected float boosterLength = 4.5f;
    protected float retreatHealth = 10f;
    protected float maxAim = 30f;

    public FlyingUnitType(String name) {
        super(name);
        speed = 0.2f;
        maxVelocity = 2f;
        drag = 0.01f;
        isFlying = true;
    }

    @Override
    public void update(BaseUnit unit) {
        super.update(unit);

        unit.rotation = unit.velocity.angle();
        unit.state.update(unit);
    }

    @Override
    public void draw(BaseUnit unit) {
        Draw.alpha(unit.hitTime / Unit.hitDuration);

        Draw.rect(name, unit.x, unit.y, unit.rotation - 90);

        Draw.alpha(1f);
    }

    @Override
    public void behavior(BaseUnit unit) {

    }

    protected void circle(BaseUnit unit, float circleLength){
        vec.set(unit.target.x - unit.x, unit.target.y - unit.y);

        if(vec.len() < circleLength){
            vec.rotate((circleLength-vec.len())/circleLength * 180f);
        }

        vec.setLength(speed * Timers.delta());

        unit.velocity.add(vec);
    }

    protected void attack(BaseUnit unit, float circleLength){
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
            if(unit.target != null && (unit.target instanceof TileEntity &&
                    (((TileEntity)unit.target).tile.getTeam() == unit.team || !((TileEntity)unit.target).tile.breakable()))){
                unit.target = null;
            }

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

                    shoot(unit, ammo, Angles.moveToward(unit.rotation, unit.angleTo(unit.target), maxAim), 4f);
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
