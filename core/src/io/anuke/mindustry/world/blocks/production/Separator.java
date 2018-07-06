package io.anuke.mindustry.world.blocks.production;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.production.GenericCrafter.GenericCrafterEntity;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;
import io.anuke.mindustry.world.meta.values.ItemFilterValue;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;

/**Extracts a random list of items from an input item and an input liquid.*/
public class Separator extends Block {
    protected final int timerDump = timers ++;

    protected Liquid liquid;
    protected Item item;
    protected Item[] results;
    protected float liquidUse;
    protected float powerUse;
    protected float filterTime;
    protected float spinnerRadius = 2.5f;
    protected float spinnerLength = 1f;
    protected float spinnerThickness = 1f;
    protected float spinnerSpeed = 2f;

    protected boolean offloading = false;

    public Separator(String name) {
        super(name);
        update = true;
        solid = true;
        hasItems = true;
        hasLiquids = true;
    }

    @Override
    public void setStats() {
        super.setStats();

        if(hasPower){
            stats.add(BlockStat.powerUse, powerUse * 60f, StatUnit.powerSecond);
        }

        stats.add(BlockStat.liquidUse, liquidUse * 60f, StatUnit.liquidSecond);
        stats.add(BlockStat.inputLiquid, liquid);
        stats.add(BlockStat.inputItem, item);
        stats.add(BlockStat.outputItem, new ItemFilterValue(item -> {
            for(Item i : results){
                if(item == i) return true;
            }
            return false;
        }));
    }

    @Override
    public void draw(Tile tile) {
        super.draw(tile);

        GenericCrafterEntity entity = tile.entity();

        Draw.color(tile.entity.liquids.liquid.color);
        Draw.alpha(tile.entity.liquids.amount / liquidCapacity);
        Draw.rect(name + "-liquid", tile.drawx(), tile.drawy());

        Draw.color(Color.valueOf("858585"));
        Lines.stroke(spinnerThickness);
        Lines.spikes(tile.drawx(), tile.drawy(), spinnerRadius, spinnerLength, 3, entity.totalProgress*spinnerSpeed);
        Draw.reset();
    }

    @Override
    public void update(Tile tile) {
        GenericCrafterEntity entity = tile.entity();

        float liquidUsed = Math.min(liquidCapacity, liquidUse * Timers.delta());
        float powerUsed = Math.min(powerCapacity, powerUse * Timers.delta());

        entity.totalProgress += entity.warmup*Timers.delta();

        if(entity.liquids.amount >= liquidUsed && entity.items.hasItem(item) &&
                (!hasPower || entity.power.amount >= powerUsed)){
            entity.progress += 1f/filterTime;
            entity.liquids.amount -= liquidUsed;
            if(hasPower) entity.power.amount -= powerUsed;

            entity.warmup = Mathf.lerpDelta(entity.warmup, 1f, 0.02f);
        }else{
            entity.warmup = Mathf.lerpDelta(entity.warmup, 0f, 0.02f);
        }

        if(entity.progress >= 1f){
            entity.progress = 0f;
            Item item = Mathf.select(results);
            entity.items.removeItem(this.item, 1);
            if(item != null){
                offloading = true;
                offloadNear(tile, item);
                offloading = false;
            }
        }

        if(entity.timer.get(timerDump, 5)){
            tryDump(tile);
        }
    }

    @Override
    public boolean canDump(Tile tile, Tile to, Item item) {
        return offloading || item != this.item;
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        return super.acceptLiquid(tile, source, liquid, amount) && this.liquid == liquid;
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source) {
        return this.item == item && tile.entity.items.getItem(item) < itemCapacity;
    }

    @Override
    public TileEntity getEntity() {
        return new GenericCrafterEntity();
    }
}
