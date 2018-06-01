package io.anuke.mindustry.entities.units.types;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.FlyingUnitType;
import io.anuke.mindustry.entities.units.UnitState;
import io.anuke.mindustry.world.BlockFlag;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Shapes;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.world;

public class Drone extends FlyingUnitType {
    protected float healSpeed = 0.1f;
    protected float discoverRange = 120f;

    public Drone() {
        super("drone");
        speed = 0.2f;
        maxVelocity = 0.8f;
        range = 50f;
    }

    @Override
    public void update(BaseUnit unit) {
        float rot = unit.rotation;
        super.update(unit);
        unit.rotation = rot;

        if(unit.target != null && unit.state.is(repair)){
            unit.rotation = Mathf.slerpDelta(rot, unit.angleTo(unit.target), 0.3f);
        }else{
            unit.rotation = Mathf.slerpDelta(rot, unit.velocity.angle(), 0.3f);
        }

        unit.x += Mathf.sin(Timers.time() + unit.id * 999, 25f, 0.07f);
        unit.y += Mathf.cos(Timers.time() + unit.id * 999, 25f, 0.07f);

        if(unit.velocity.len() <= 0.2f && !(unit.state.is(repair) && unit.target != null)){
            unit.rotation += Mathf.sin(Timers.time() + unit.id * 99, 10f, 5f);
        }
    }

    @Override
    public void behavior(BaseUnit unit) {
        if(unit.health <= health * retreatPercent &&
                Geometry.findClosest(unit.x, unit.y, world.indexer().getAllied(unit.team, BlockFlag.repair)) != null){
            unit.setState(retreat);
        }
    }

    @Override
    public UnitState getStartState() {
        return repair;
    }

    @Override
    public void drawOver(BaseUnit unit) {
        if(unit.target instanceof TileEntity && unit.state.is(repair)){
            float len = 5f;
            Draw.color(Color.BLACK, Color.WHITE, 0.95f + Mathf.absin(Timers.time(), 0.8f, 0.05f));
            Shapes.laser("beam", "beam-end",
                    unit.x + Angles.trnsx(unit.rotation, len),
                    unit.y + Angles.trnsy(unit.rotation, len),
                    unit.target.getX(), unit.target.getY());
            Draw.color();
        }
    }

    public final UnitState

    repair = new UnitState(){
        public void entered(BaseUnit unit) {
            unit.target = null;
        }

        public void update(BaseUnit unit) {
            if(unit.target != null && (((TileEntity)unit.target).health >= ((TileEntity)unit.target).tile.block().health
                    || unit.target.distanceTo(unit) > discoverRange)){
                unit.target = null;
            }

            if (unit.target == null) {
                if (unit.timer.get(timerTarget, 20)) {
                    unit.target = Units.findAllyTile(unit.team, unit.x, unit.y, discoverRange,
                           tile -> tile.entity != null && tile.entity.health + 0.0001f < tile.block().health);
                }
            }else if(unit.target.distanceTo(unit) > range){
                circle(unit, range);
            }else{
                TileEntity entity = (TileEntity) unit.target;
                entity.health += healSpeed * Timers.delta();
                entity.health = Mathf.clamp(entity.health, 0, entity.tile.block().health);
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
                circle(unit, 40f);
            }
        }
    };

}
