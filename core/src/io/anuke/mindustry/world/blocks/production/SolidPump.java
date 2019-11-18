package io.anuke.mindustry.world.blocks.production;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.Effects.Effect;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.ui.Bar;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.Attribute;
import io.anuke.mindustry.world.meta.BlockStat;

/**
 * Pump that makes liquid from solids and takes in power. Only works on solid floor blocks.
 */
public class SolidPump extends Pump{
    protected Liquid result = Liquids.water;
    protected Effect updateEffect = Fx.none;
    protected float updateEffectChance = 0.02f;
    protected float rotateSpeed = 1f;
    /** Attribute that is checked when calculating output. */
    protected Attribute attribute;

    public SolidPump(String name){
        super(name);
        hasPower = true;
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
        Core.bundle.formatFloat("bar.efficiency",
        ((((SolidPumpEntity)entity).boost + 1f) * ((SolidPumpEntity)entity).warmup) * 100  * percentSolid(entity.tile.x, entity.tile.y), 1),
        () -> Pal.ammo,
        () -> ((SolidPumpEntity)entity).warmup));
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(BlockStat.output);
        stats.add(BlockStat.output, result, 60f * pumpAmount, true);
    }

    @Override
    public void draw(Tile tile){
        SolidPumpEntity entity = tile.entity();

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
        SolidPumpEntity entity = tile.entity();

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
            entity.warmup = Mathf.lerpDelta(entity.warmup, 1f, 0.02f);
            if(Mathf.chance(entity.delta() * updateEffectChance))
                Effects.effect(updateEffect, entity.x + Mathf.range(size * 2f), entity.y + Mathf.range(size * 2f));
        }else{
            entity.warmup = Mathf.lerpDelta(entity.warmup, 0f, 0.02f);
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
    public TileEntity newEntity(){
        return new SolidPumpEntity();
    }

    @Override
    public void onProximityAdded(Tile tile){
        super.onProximityAdded(tile);

        if(attribute != null){
            SolidPumpEntity entity = tile.entity();
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
    }
}
