package io.anuke.mindustry.world.blocks.units;

import io.anuke.arc.Core;
import io.anuke.arc.collection.EnumSet;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.Angles;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Rectangle;
import io.anuke.arc.util.*;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.entities.type.Unit;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.*;

import static io.anuke.mindustry.Vars.tilesize;

public class RepairPoint extends Block{
    protected static Rectangle rect = new Rectangle();

    public int timerTarget = timers++;

    public float repairRadius = 50f;
    public float repairSpeed = 0.3f;
    public float powerUse;
    public TextureRegion baseRegion;
    public TextureRegion laser, laserEnd;
    public Color laserColor;

    public RepairPoint(String name){
        super(name);
        update = true;
        solid = true;
        flags = EnumSet.of(BlockFlag.repair);
        layer = Layer.turret;
        layer2 = Layer.power;
        hasPower = true;
        outlineIcon = true;
        laserColor = Color.valueOf("e8ffd7");
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
        Drawf.circles(tile.drawx(), tile.drawy(), repairRadius, Pal.accent);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Drawf.circles(x * tilesize + offset(), y * tilesize + offset(), repairRadius, Pal.accent);
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
        Angles.angleDist(entity.angleTo(entity.target), entity.rotation) < 30f && entity.laser){
            float ang = entity.angleTo(entity.target);
            float len = 5f;

            Draw.color(laserColor);
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
        if(entity.target != null && (entity.target.isDead() || entity.target.dst(tile) > repairRadius)){
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
                    unit -> true);
        }
    }

    @Override
    public boolean shouldConsume(Tile tile){
        RepairPointEntity entity = tile.ent();

        return entity.target != null;
    }

    public class RepairPointEntity extends TileEntity{
        public Unit target;
        public boolean laser = true;
        public float strength, rotation = 90;
    }
}
