package io.anuke.mindustry.entities.units;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.game.TeamInfo.TeamData;
import io.anuke.mindustry.resource.AmmoType;
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

import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.world;

public abstract class GroundUnitType extends UnitType{
    protected static Translator vec = new Translator();

    protected float maxAim = 30f;

    public GroundUnitType(String name) {
        super(name);
        maxVelocity = 1.1f;
        speed = 0.1f;
        drag = 0.4f;
        range = 40f;
    }

    @Override
    public UnitState getStartState() {
        return resupply;
    }

    @Override
    public void update(BaseUnit unit) {
        super.update(unit);

        unit.rotation = unit.velocity.angle();
    }

    @Override
    public void draw(BaseUnit unit) {
        Draw.alpha(unit.hitTime / Unit.hitDuration);

        float walktime = unit.walkTime;

        float ft = Mathf.sin(walktime, 6f, 2f);

        Floor floor = unit.getFloorOn();

        if(floor.liquid){
            Draw.tint(Hue.mix(Color.WHITE, floor.liquidColor, 0.5f));
        }

        for (int i : Mathf.signs) {
            Draw.rect(name + "-leg",
                    unit.x + Angles.trnsx(unit.baseRotation, ft * i),
                    unit.y + Angles.trnsy(unit.baseRotation, ft * i),
                    12f * i, 12f - Mathf.clamp(ft * i, 0, 2), unit.baseRotation - 90);
        }

        if(floor.liquid) {
            Draw.tint(Color.WHITE, floor.liquidColor, unit.drownTime * 0.4f);
        }else {
            Draw.tint(Color.WHITE);
        }

        Draw.rect(name + "-base", unit.x, unit.y, unit.baseRotation- 90);

        Draw.rect(name, unit.x, unit.y, unit.rotation -90);

        Draw.alpha(1f);
    }

    @Override
    public void updateTargeting(BaseUnit unit) {
        if(!unit.timer.get(timerTarget, 20)) return;

        unit.target = Units.findEnemyTile(unit.team, unit.x, unit.y, range, t -> true);

        if(unit.target != null) return;

        ObjectSet<TeamData> teams = state.teams.enemyDataOf(unit.team);

        Tile closest = null;
        float cdist = 0f;

        for(TeamData data : teams){
            for(Tile tile : data.cores){
                float dist = Vector2.dst(unit.x, unit.y, tile.drawx(), tile.drawy());
                if(closest == null || dist < cdist){
                    closest = tile;
                    cdist = dist;
                }
            }
        }

        if(closest != null){
            unit.target = closest.entity;
        }else{
            unit.target = null;
        }
    }

    @Override
    public void behavior(BaseUnit unit) {
        if(unit.health <= health * retreatPercent){
            unit.setState(retreat);
        }
    }

    protected void moveToCore(BaseUnit unit){
        Tile tile = world.tileWorld(unit.x, unit.y);
        Tile targetTile = world.pathfinder().getTargetTile(unit.team, tile);

        if(tile == targetTile) return;

        vec.trns(unit.baseRotation, speed);

        unit.baseRotation = Mathf.slerpDelta(unit.baseRotation, unit.angleTo(targetTile), 0.05f);
        unit.walkTime += Timers.delta();
        unit.velocity.add(vec);
    }

    protected void moveAwayFromCore(BaseUnit unit){
        Tile tile = world.tileWorld(unit.x, unit.y);
        Tile targetTile = world.pathfinder().getTargetTile(state.teams.enemiesOf(unit.team).first(), tile);

        if(tile == targetTile) return;

        vec.trns(unit.baseRotation, speed);

        unit.baseRotation = Mathf.slerpDelta(unit.baseRotation, unit.angleTo(targetTile), 0.05f);
        unit.walkTime += Timers.delta();
        unit.velocity.add(vec);
    }

    public final UnitState

    resupply = new UnitState(){
        public void entered(BaseUnit unit) {
            unit.target = null;
        }

        public void update(BaseUnit unit) {
            //TODO move toward resupply point?
            if(unit.inventory.totalAmmo() + 10 >= unit.inventory.ammoCapacity()){
                unit.state.set(unit, attack);
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
                moveToCore(unit);

                if (unit.timer.get(timerReload, reload) && Mathf.angNear(unit.angleTo(unit.target), unit.rotation, 13f)
                        && unit.distanceTo(unit.target) < unit.inventory.getAmmo().getRange()) {
                    AmmoType ammo = unit.inventory.getAmmo();
                    unit.inventory.useAmmo();
                    unit.rotate(unit.angleTo(unit.target));

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
            }

            moveAwayFromCore(unit);
        }
    };
}
