package io.anuke.mindustry.world.blocks.units;

import io.anuke.arc.Core;
import io.anuke.arc.collection.EnumSet;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.Angles;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Rectangle;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.entities.type.Unit;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockFlag;

public class RepairPoint extends Block{
    private static Rectangle rect = new Rectangle();

    protected int timerTarget = timers++;

    protected float repairRadius = 50f;
    protected float repairSpeed = 0.3f;
    protected float powerUse;
    protected TextureRegion baseRegion;
    protected TextureRegion laser, laserEnd;

    public RepairPoint(String name){
        super(name);
        update = true;
        solid = true;
        flags = EnumSet.of(BlockFlag.repair);
        layer = Layer.turret;
        layer2 = Layer.power;
        hasPower = true;
        outlineIcon = true;
    }

    @Override
    public void load(){
        super.load();

        baseRegion = Core.atlas.find(name + "-base");
        laser = Core.atlas.find("laser");
        laserEnd = Core.atlas.find("laser-end");
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
    public void draw(Tile tile){
        Draw.rect(baseRegion, tile.drawx(), tile.drawy());
    }

    @Override
    public void drawLayer(Tile tile){
        RepairPointEntity entity = tile.entity();

        Draw.rect(region, tile.drawx(), tile.drawy(), entity.rotation - 90);
    }

    @Override
    public void drawLayer2(Tile tile){
        RepairPointEntity entity = tile.entity();

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
        RepairPointEntity entity = tile.entity();

        boolean targetIsBeingRepaired = false;
        if(entity.target != null && (entity.target.isDead() || entity.target.dst(tile) > repairRadius || entity.target.health >= entity.target.maxHealth())){
            entity.target = null;
        }else if(entity.target != null && entity.cons.valid()){
            entity.target.health += repairSpeed * Time.delta() * entity.strength * entity.power.satisfaction;
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
