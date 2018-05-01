package io.anuke.mindustry.world.blocks.types.units;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.flags.BlockFlag;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.graphics.Shapes;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.EnumSet;
import io.anuke.ucore.util.Mathf;

public class RepairPoint extends Block{
    private static Rectangle rect = new Rectangle();

    protected int timerTarget = timers ++;

    protected float repairRadius = 50f;
    protected float repairSpeed = 0.3f;
    protected float powerUsage = 0.2f;

    public RepairPoint(String name) {
        super(name);
        update = true;
        solid = true;
        flags = EnumSet.of(BlockFlag.repair);
        layer = Layer.turret;
        layer2 = Layer.laser;
        hasItems = false;
        hasPower = true;
        powerCapacity = 20f;
    }

    @Override
    public void drawSelect(Tile tile){
        Draw.color("accent");
        Lines.dashCircle(tile.drawx(), tile.drawy(), repairRadius);
        Draw.color();
    }

    @Override
    public void drawLayer(Tile tile) {
        RepairPointEntity entity = tile.entity();

        Draw.rect(name + "-turret", tile.drawx(), tile.drawy(), entity.rotation - 90);
    }

    @Override
    public void drawLayer2(Tile tile) {
        RepairPointEntity entity = tile.entity();

        if(entity.target != null &&
                Angles.angleDist(entity.angleTo(entity.target), entity.rotation) < 30f){
            float ang = entity.angleTo(entity.target);
            float len = 5f;

            Draw.color(Color.valueOf("e8ffd7"));
            Shapes.laser("laser", "laser-end",
                    tile.drawx() + Angles.trnsx(ang, len), tile.drawy() + Angles.trnsy(ang, len),
                    entity.target.x, entity.target.y, entity.strength);
            Draw.color();
        }
    }

    @Override
    public void update(Tile tile) {
        RepairPointEntity entity = tile.entity();

        if(entity.target != null && (entity.target.isDead() || entity.target.distanceTo(tile) > repairRadius ||
            entity.target.health >= entity.target.maxhealth)){
            entity.target = null;
        }else if(entity.target != null){
            entity.target.health += repairSpeed * Timers.delta() * entity.strength;
            entity.target.clampHealth();
            entity.rotation = Mathf.slerpDelta(entity.rotation, entity.angleTo(entity.target), 0.5f);
        }

        float powerUse = Math.min(Timers.delta() * powerUsage, powerCapacity);

        if(entity.target != null && entity.power.amount >= powerUse){
            entity.power.amount -= powerUse;
            entity.strength = Mathf.lerpDelta(entity.strength, 1f, 0.08f * Timers.delta());
        }else{
            entity.strength = Mathf.lerpDelta(entity.strength, 0f, 0.07f * Timers.delta());
        }

        if(entity.timer.get(timerTarget, 20)) {
            rect.setSize(repairRadius * 2).setCenter(tile.drawx(), tile.drawy());
            entity.target = Units.getClosest(tile.getTeam(), tile.drawx(), tile.drawy(), repairRadius,
                    unit -> unit.health < unit.maxhealth);
        }
    }

    @Override
    public TileEntity getEntity() {
        return new RepairPointEntity();
    }

    public class RepairPointEntity extends TileEntity{
        public Unit target;
        public float strength, rotation = 90;
    }
}
