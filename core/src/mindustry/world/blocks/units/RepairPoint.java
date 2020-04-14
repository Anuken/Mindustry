package mindustry.world.blocks.units;

import arc.Core;
import arc.struct.EnumSet;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Rect;
import arc.util.Time;
import mindustry.entities.Units;
import mindustry.entities.type.TileEntity;
import mindustry.entities.type.Unit;
import mindustry.graphics.*;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.*;

import static mindustry.Vars.tilesize;

public class RepairPoint extends Block{
    private static Rect rect = new Rect();

    public int timerTarget = timers++;

    public float repairRadius = 50f;
    public float repairSpeed = 0.3f;
    public float powerUse;
    public TextureRegion baseRegion;
    public TextureRegion laser, laserEnd;

    public RepairPoint(String name){
        super(name);
        update = true;
        solid = true;
        flags = EnumSet.of(BlockFlag.repair);
        layer = Layer.turret;
        layer2 = Layer.power;
        hasPower = true;
        outlineIcon = true;
        entityType = RepairPointEntity::new;
    }

    @Override
    public void load(){
        super.load();

        baseRegion = Core.atlas.find(name + "-base");
        laser = Core.atlas.find("laser");
        laserEnd = Core.atlas.find("laser-end");
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(BlockStat.range, repairRadius / tilesize, StatUnit.blocks);
    }

    @Override
    public void init(){
        consumes.powerCond(powerUse, entity -> ((RepairPointEntity)entity).target != null);
        super.init();
    }

    @Override
    public void drawSelect(Tile tile){
        Drawf.dashCircle(tile.drawx(), tile.drawy(), repairRadius, Pal.accent);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Drawf.dashCircle(x * tilesize + offset(), y * tilesize + offset(), repairRadius, Pal.accent);
    }

    @Override
    public void draw(Tile tile){
        Draw.rect(baseRegion, tile.drawx(), tile.drawy());
    }

    @Override
    public void drawLayer(Tile tile){
        RepairPointEntity entity = tile.ent();

        Draw.rect(region, tile.drawx(), tile.drawy(), entity.rotation - 90);
    }

    @Override
    public void drawLayer2(Tile tile){
        RepairPointEntity entity = tile.ent();

        if(entity.target != null &&
        Angles.angleDist(entity.angleTo(entity.target), entity.rotation) < 30f){
            float ang = entity.angleTo(entity.target);
            float len = 5f;

            Draw.color(Color.valueOf("e8ffd7"));
            Drawf.laser(laser, laserEnd,
                tile.drawx() + Angles.trnsx(ang, len), tile.drawy() + Angles.trnsy(ang, len),
                entity.target.x, entity.target.y, entity.strength);
            Draw.color();
        }
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name + "-base"), Core.atlas.find(name)};
    }

    @Override
    public void update(Tile tile){
        RepairPointEntity entity = tile.ent();

        boolean targetIsBeingRepaired = false;
        if(entity.target != null && (entity.target.isDead() || entity.target.dst(tile) > repairRadius || entity.target.health >= entity.target.maxHealth())){
            entity.target = null;
        }else if(entity.target != null && entity.cons.valid()){
            entity.target.health += repairSpeed * Time.delta() * entity.strength * entity.efficiency();
            entity.target.clampHealth();
            entity.rotation = Mathf.slerpDelta(entity.rotation, entity.angleTo(entity.target), 0.5f);
            targetIsBeingRepaired = true;
        }

        if(entity.target != null && targetIsBeingRepaired){
            entity.strength = Mathf.lerpDelta(entity.strength, 1f, 0.08f * Time.delta());
        }else{
            entity.strength = Mathf.lerpDelta(entity.strength, 0f, 0.07f * Time.delta());
        }

        if(entity.timer.get(timerTarget, 20)){
            rect.setSize(repairRadius * 2).setCenter(tile.drawx(), tile.drawy());
            entity.target = Units.closest(tile.getTeam(), tile.drawx(), tile.drawy(), repairRadius,
            unit -> unit.health < unit.maxHealth());
        }
    }

    @Override
    public boolean shouldConsume(Tile tile){
        RepairPointEntity entity = tile.ent();

        return entity.target != null;
    }

    public class RepairPointEntity extends TileEntity{
        public Unit target;
        public float strength, rotation = 90;
    }
}
