package io.anuke.mindustry.world.blocks.types.production;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.production.GenericCrafter.GenericCrafterEntity;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

public class PowerCrafter extends Block{
    protected final int timerDump = timers++;

    /**Required.*/
    protected ItemStack input;
    /**Optional.*/
    protected Item outputItem;
    /**Optional. Set hasLiquids to true when using.*/
    protected Liquid outputLiquid;
    protected float outputLiquidAmount;
    protected float powerUse;
    protected float craftTime;

    public PowerCrafter(String name) {
        super(name);
        solid = true;
        update = true;
        hasPower = true;
    }

    @Override
    public void update(Tile tile) {
        GenericCrafterEntity entity = tile.entity();

        float powerUsed = Math.min(Timers.delta() * powerUse, tile.entity.power.amount);
        int itemsUsed = Mathf.ceil(1 + input.amount * entity.progress);

        if(entity.power.amount > powerUsed && entity.inventory.hasItem(input.item, itemsUsed)){
            entity.progress += 1f/craftTime;
            entity.totalProgress += Timers.delta();
        }

        if(entity.progress >= 1f){
            entity.inventory.removeItem(input);
            if(outputItem != null) offloadNear(tile, outputItem);
            if(outputLiquid != null) handleLiquid(tile, tile, outputLiquid, outputLiquidAmount);
            entity.progress = 0f;
        }

        if(outputItem != null && entity.timer.get(timerDump, 5)){
            tryDump(tile, outputItem);
        }

        if(outputLiquid != null){
            tryDumpLiquid(tile);
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source) {
        return item == input.item && tile.entity.inventory.getItem(input.item) < itemCapacity;
    }

    @Override
    public TileEntity getEntity() {
        return new GenericCrafterEntity();
    }
}
