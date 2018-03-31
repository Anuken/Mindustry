package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.production.GenericCrafter.GenericCrafterEntity;
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
        hasInventory = true;
        hasLiquids = true;
    }

    @Override
    public void draw(Tile tile) {
        super.draw(tile);

        GenericCrafterEntity entity = tile.entity();

        Draw.color(tile.entity.liquid.liquid.color);
        Draw.alpha(tile.entity.liquid.amount / liquidCapacity);
        Draw.rect(name + "-liquid", tile.drawx(), tile.drawy());

        Draw.color(Color.valueOf("858585"));
        Lines.stroke(spinnerThickness);
        Lines.spikes(tile.drawx(), tile.drawy(), spinnerRadius, spinnerLength, 3, entity.craftTime*spinnerSpeed);
        Draw.reset();
    }

    //TODO draw with effects such as spinning

    @Override
    public void update(Tile tile) {
        GenericCrafterEntity entity = tile.entity();

        float liquidUsed = Math.min(liquidCapacity, liquidUse * Timers.delta());
        float powerUsed = Math.min(powerCapacity, powerUse * Timers.delta());

        entity.craftTime += entity.warmup*Timers.delta();

        if(entity.liquid.amount >= liquidUsed && entity.inventory.hasItem(item) &&
                (!hasPower || entity.power.amount >= powerUsed)){
            entity.progress += 1f/filterTime;
            entity.liquid.amount -= liquidUsed;
            if(hasPower) entity.power.amount -= powerUsed;

            entity.warmup = Mathf.lerpDelta(entity.warmup, 1f, 0.02f);
        }else{
            entity.warmup = Mathf.lerpDelta(entity.warmup, 0f, 0.02f);
        }

        if(entity.progress >= 1f){
            entity.progress = 0f;
            Item item = Mathf.select(results);
            entity.inventory.removeItem(this.item, 1);
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
        return this.item == item && tile.entity.inventory.getItem(item) < itemCapacity;
    }

    @Override
    public TileEntity getEntity() {
        return new GenericCrafterEntity();
    }
}
