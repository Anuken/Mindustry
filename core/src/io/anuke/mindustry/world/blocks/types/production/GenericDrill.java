package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

/**Generic drill, can use both power and liquids. Set 'resource' to null to make it drill any block with drops.*/
public class GenericDrill extends Drill{
    /**power use per frame.*/
    public float powerUse = 0.08f;
    /**liquid use per frame.*/
    protected float liquidUse = 0.1f;
    protected Liquid inputLiquid = Liquids.water;

    protected float rotateSpeed = 1.5f;

    public GenericDrill(String name){
        super(name);
        updateEffect = Fx.pulverizeMedium;
    }

    @Override
    public void draw(Tile tile) {
        DrillEntity entity = tile.entity();

        Draw.rect(name, tile.drawx(), tile.drawy());
        Draw.rect(name + "-rotator", tile.drawx(), tile.drawy(), entity.drillTime * rotateSpeed);
        Draw.rect(name + "-top", tile.drawx(), tile.drawy());
    }

    @Override
    public TextureRegion[] getIcon() {
        return new TextureRegion[]{Draw.region(name), Draw.region(name + "-rotator"), Draw.region(name + "-top")};
    }

    @Override
    public void update(Tile tile){
        toAdd.clear();

        DrillEntity entity = tile.entity();

        float multiplier = 0f;
        float totalHardness = 0f;

        for(Tile other : tile.getLinkedTiles(tempTiles)){
            if(isValid(other)){
                toAdd.add(other.floor().drops.item);
                totalHardness += other.floor().drops.item.hardness;
                multiplier += 1f;
            }
        }

        entity.drillTime += entity.warmup * Timers.delta();

        float powerUsed = Math.min(powerCapacity, powerUse * Timers.delta());
        float liquidUsed = Math.min(liquidCapacity, liquidUse * Timers.delta());

        //TODO slow down when no space.
        if((!hasPower || entity.power.amount >= powerUsed)
                && (!hasLiquids || entity.liquid.amount >= liquidUsed)){
            if(hasPower) entity.power.amount -= powerUsed;
            if(hasLiquids) entity.liquid.amount -= liquidUsed;
            entity.warmup = Mathf.lerpDelta(entity.warmup, 1f, 0.02f);
            entity.progress += Timers.delta() * multiplier;

            if(Mathf.chance(Timers.delta() * updateEffectChance))
                Effects.effect(updateEffect, entity.x + Mathf.range(size*2f), entity.y + Mathf.range(size*2f));
        }else{
            entity.warmup = Mathf.lerpDelta(entity.warmup, 0f, 0.02f);
            return;
        }

        if(toAdd.size > 0 && entity.progress >= drillTime + hardnessDrillMultiplier*totalHardness
                && tile.entity.inventory.totalItems() < itemCapacity){

            int index = entity.index % toAdd.size;
            offloadNear(tile, toAdd.get(index));

            entity.index ++;
            entity.progress = 0f;

            Effects.effect(drillEffect, toAdd.get(index).color, tile.drawx(), tile.drawy());
        }

        if(entity.timer.get(timerDump, 15)){
            tryDump(tile);
        }
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        return super.acceptLiquid(tile, source, liquid, amount) && liquid == inputLiquid;
    }

    @Override
    public TileEntity getEntity() {
        return new DrillEntity();
    }

    public static class DrillEntity extends TileEntity{
        public float progress;
        public int index;
        public float warmup;
        public float drillTime;
    }
}
