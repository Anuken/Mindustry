package io.anuke.mindustry.world.blocks.production;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

/**
 * Pump that makes liquid from solids and takes in power. Only works on solid floor blocks.
 */
public class SolidPump extends Pump{
    protected Liquid result = Liquids.water;
    protected Effect updateEffect = Fx.none;
    protected float updateEffectChance = 0.02f;
    protected float rotateSpeed = 1f;

    public SolidPump(String name){
        super(name);
        hasPower = true;
    }

    @Override
    public void load(){
        super.load();

        liquidRegion = Draw.region(name + "-liquid");
    }

    @Override
    public void setStats(){
        super.setStats();

        // stats.remove(BlockStat.liquidOutput);
        stats.add(BlockStat.liquidOutput, result);
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
    public TextureRegion[] getIcon(){
        return new TextureRegion[]{Draw.region(name), Draw.region(name + "-rotator"), Draw.region(name + "-top")};
    }

    @Override
    public void update(Tile tile){
        SolidPumpEntity entity = tile.entity();

        float fraction = 0f;

        if(isMultiblock()){
            for(Tile other : tile.getLinkedTiles(tempTiles)){
                if(isValid(other)){
                    fraction += 1f / size;
                }
            }
        }else{
            if(isValid(tile)) fraction = 1f;
        }

        if(tile.entity.cons.valid() && typeLiquid(tile) < liquidCapacity - 0.001f){
            float maxPump = Math.min(liquidCapacity - typeLiquid(tile), pumpAmount * entity.delta() * fraction * entity.power.satisfaction);
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

    public float typeLiquid(Tile tile){
        return tile.entity.liquids.total();
    }

    public static class SolidPumpEntity extends TileEntity{
        public float warmup;
        public float pumpTime;
    }
}
