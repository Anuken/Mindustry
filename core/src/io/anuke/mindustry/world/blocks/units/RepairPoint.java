package io.anuke.mindustry.world.blocks.units;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.ConsumePower;
import io.anuke.mindustry.world.meta.BlockFlag;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.graphics.Shapes;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.EnumSet;
import io.anuke.ucore.util.Mathf;

public class RepairPoint extends Block{
    private static Rectangle rect = new Rectangle();

    protected int timerTarget = timers++;

    protected float repairRadius = 50f;
    protected float repairSpeed = 0.3f;
    protected float powerPerEvent = 0.06f;
    protected ConsumePower consumePower;

    protected TextureRegion topRegion;

    public RepairPoint(String name){
        super(name);
        update = true;
        solid = true;
        flags = EnumSet.of(BlockFlag.repair);
        layer = Layer.turret;
        layer2 = Layer.laser;
        hasPower = true;
        consumePower = consumes.powerBuffered(20f);
    }

    @Override
    public void load(){
        super.load();

        topRegion = Draw.region(name + "-turret");
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(BlockStat.powerUse, powerPerEvent * 60f, StatUnit.powerSecond);
    }

    @Override
    public void drawSelect(Tile tile){
        Draw.color(Palette.accent);
        Lines.dashCircle(tile.drawx(), tile.drawy(), repairRadius);
        Draw.color();
    }

    @Override
    public void drawLayer(Tile tile){
        RepairPointEntity entity = tile.entity();

        Draw.rect(topRegion, tile.drawx(), tile.drawy(), entity.rotation - 90);
    }

    @Override
    public void drawLayer2(Tile tile){
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
    public void update(Tile tile){
        RepairPointEntity entity = tile.entity();

        boolean targetIsBeingRepaired = false;
        if(entity.target != null && (entity.target.isDead() || entity.target.distanceTo(tile) > repairRadius ||
                entity.target.health >= entity.target.maxHealth())){
            entity.target = null;
        }else if(entity.target != null){
            float relativeConsumption = powerPerEvent / consumePower.powerCapacity;
            if(entity.power.satisfaction > 0.0f){
                entity.target.health += repairSpeed * Timers.delta() * entity.strength * Mathf.clamp(entity.power.satisfaction / relativeConsumption);
                entity.target.clampHealth();
                entity.rotation = Mathf.slerpDelta(entity.rotation, entity.angleTo(entity.target), 0.5f);
                entity.power.satisfaction -= Math.min(entity.power.satisfaction, relativeConsumption);
                targetIsBeingRepaired = true;
            }
        }

        if(entity.target != null && targetIsBeingRepaired){
            entity.strength = Mathf.lerpDelta(entity.strength, 1f, 0.08f * Timers.delta());
        }else{
            entity.strength = Mathf.lerpDelta(entity.strength, 0f, 0.07f * Timers.delta());
        }

        if(entity.timer.get(timerTarget, 20)){
            rect.setSize(repairRadius * 2).setCenter(tile.drawx(), tile.drawy());
            entity.target = Units.getClosest(tile.getTeam(), tile.drawx(), tile.drawy(), repairRadius,
                    unit -> unit.health < unit.maxHealth());
        }
    }

    @Override
    public boolean shouldConsume(Tile tile){
        RepairPointEntity entity = tile.entity();

        return entity.target != null;
    }

    @Override
    public TileEntity newEntity(){
        return new RepairPointEntity();
    }

    public class RepairPointEntity extends TileEntity{
        public Unit target;
        public float strength, rotation = 90;
    }
}
