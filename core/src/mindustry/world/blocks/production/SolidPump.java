package mindustry.world.blocks.production;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.*;
import arc.util.ArcAnnotate.*;
import mindustry.content.Fx;
import mindustry.content.Liquids;
import mindustry.entities.Effects;
import mindustry.entities.Effects.Effect;
import mindustry.entities.type.TileEntity;
import mindustry.graphics.Pal;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.world.Tile;
import mindustry.world.meta.Attribute;
import mindustry.world.meta.BlockStat;

/**
 * Pump that makes liquid from solids and takes in power. Only works on solid floor blocks.
 */
public class SolidPump extends Pump{
    public Liquid result = Liquids.water;
    public Effect updateEffect = Fx.none;
    public float updateEffectChance = 0.02f;
    public float rotateSpeed = 1f;
    /** Attribute that is checked when calculating output. */
    public @Nullable Attribute attribute;

    public SolidPump(String name){
        super(name);
        hasPower = true;
        entityType = SolidPumpEntity::new;
    }

    @Override
    public void load(){
        super.load();

        liquidRegion = Core.atlas.find(name + "-liquid");
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        if(attribute != null){
            drawPlaceText(Core.bundle.formatFloat("bar.efficiency", (sumAttribute(attribute, x, y) + 1f) * 100 * percentSolid(x, y), 1), x, y, valid);
        }
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.add("efficiency", entity -> new Bar(() ->
        Core.bundle.formatFloat("bar.pumpspeed",
        ((SolidPumpEntity)entity).lastPump / Time.delta() * 60, 1),
        () -> Pal.ammo,
        () -> ((SolidPumpEntity)entity).warmup));
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(BlockStat.output);
        stats.add(BlockStat.output, result, 60f * pumpAmount, true);
        if(attribute != null){
            stats.add(BlockStat.affinities, attribute);
        }
    }

    @Override
    public void draw(Tile tile){
        SolidPumpEntity entity = tile.ent();

        Draw.rect(region, tile.drawx(), tile.drawy());
        Draw.color(tile.entity.liquids.current().color);
        Draw.alpha(tile.entity.liquids.total() / liquidCapacity);
        Draw.rect(liquidRegion, tile.drawx(), tile.drawy());
        Draw.color();
        Draw.rect(name + "-rotator", tile.drawx(), tile.drawy(), entity.pumpTime * rotateSpeed);
        Draw.rect(name + "-top", tile.drawx(), tile.drawy());
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name), Core.atlas.find(name + "-rotator"), Core.atlas.find(name + "-top")};
    }

    @Override
    public void update(Tile tile){
        SolidPumpEntity entity = tile.ent();

        float fraction = 0f;

        if(isMultiblock()){
            for(Tile other : tile.getLinkedTiles(tempTiles)){
                if(isValid(other)){
                    fraction += 1f / (size * size);
                }
            }
        }else{
            if(isValid(tile)) fraction = 1f;
        }

        fraction += entity.boost;

        if(tile.entity.cons.valid() && typeLiquid(tile) < liquidCapacity - 0.001f){
            float maxPump = Math.min(liquidCapacity - typeLiquid(tile), pumpAmount * entity.delta() * fraction * entity.efficiency());
            tile.entity.liquids.add(result, maxPump);
            entity.lastPump = maxPump;
            entity.warmup = Mathf.lerpDelta(entity.warmup, 1f, 0.02f);
            if(tile.entity.timer.get(timerContentCheck, 10)) useContent(tile, result);
            if(Mathf.chance(entity.delta() * updateEffectChance))
                Effects.effect(updateEffect, entity.x + Mathf.range(size * 2f), entity.y + Mathf.range(size * 2f));
        }else{
            entity.warmup = Mathf.lerpDelta(entity.warmup, 0f, 0.02f);
            entity.lastPump = 0f;
        }

        entity.pumpTime += entity.warmup * entity.delta();

        tryDumpLiquid(tile, result);
    }

    @Override
    public boolean canPlaceOn(Tile tile){
        if(isMultiblock()){
            for(Tile other : tile.getLinkedTilesAs(this, drawTiles)){
                if(isValid(other)){
                    return true;
                }
            }
            return false;
        }else{
            return isValid(tile);
        }
    }

    @Override
    protected boolean isValid(Tile tile){
        return tile != null && !tile.floor().isLiquid;
    }

    @Override
    public void onProximityAdded(Tile tile){
        super.onProximityAdded(tile);

        if(attribute != null){
            SolidPumpEntity entity = tile.ent();
            entity.boost = sumAttribute(attribute, tile.x, tile.y);
        }
    }

    public float typeLiquid(Tile tile){
        return tile.entity.liquids.total();
    }

    public static class SolidPumpEntity extends TileEntity{
        public float warmup;
        public float pumpTime;
        public float boost;
        public float lastPump;
    }
}
