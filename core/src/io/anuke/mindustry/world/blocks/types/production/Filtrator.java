package io.anuke.mindustry.world.blocks.types.production;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.production.GenericCrafter.GenericCrafterEntity;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

/**Extracts a random list of items from an input item and an input liquid.*/
public class Filtrator extends Block {
    protected final int timerDump = timers ++;

    protected Liquid liquid;
    protected Item item;
    protected Item[] results;
    protected float liquidUse;
    protected float filterTime;

    public Filtrator(String name) {
        super(name);
        update = true;
        solid = true;
        hasInventory = true;
        hasLiquids = true;
    }

    //TODO draw with effects such as spinning

    @Override
    public void update(Tile tile) {
        GenericCrafterEntity entity = tile.entity();

        float liquidUsed = Math.min(liquidCapacity, liquidUse * Timers.delta());

        if(entity.liquid.amount >= liquidUsed && entity.inventory.hasItem(item)){
            entity.progress += 1f/filterTime;
        }

        if(entity.progress >= 1f){
            entity.progress = 0f;
            Item item = Mathf.select(results);
            if(item != null) offloadNear(tile, item);
        }

        if(entity.timer.get(timerDump, 5)){
            tryDump(tile);
        }
    }

    @Override
    public boolean canDump(Tile tile, Tile to, Item item) {
        return item != this.item;
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
