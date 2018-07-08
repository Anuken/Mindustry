package io.anuke.mindustry.world.blocks.production;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.production.GenericCrafter.GenericCrafterEntity;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;
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
        hasItems = true;
    }

    @Override
    public void setStats() {
        super.setStats();

        stats.add(BlockStat.inputItem, input);

        if(outputItem != null){
            stats.add(BlockStat.outputItem, outputItem);
        }

        if(outputLiquid != null){
            stats.add(BlockStat.liquidOutput, outputLiquid);
        }

        if(hasPower){
            stats.add(BlockStat.powerUse, 60f * powerUse, StatUnit.powerSecond);
        }
    }

    @Override
    public void update(Tile tile) {
        GenericCrafterEntity entity = tile.entity();

        float powerUsed = Math.min(Timers.delta() * powerUse, tile.entity.power.amount);
        int itemsUsed = Mathf.ceil(1 + input.amount * entity.progress);

        if(entity.power.amount > powerUsed && entity.items.has(input.item, itemsUsed)){
            entity.progress += 1f/craftTime;
            entity.totalProgress += Timers.delta();
        }

        if(entity.progress >= 1f){
            entity.items.remove(input);
            if(outputItem != null) offloadNear(tile, outputItem);
            if(outputLiquid != null) handleLiquid(tile, tile, outputLiquid, outputLiquidAmount);
            entity.progress = 0f;
        }

        if(outputItem != null && entity.timer.get(timerDump, 5)){
            tryDump(tile, outputItem);
        }

        if(outputLiquid != null){
            tryDumpLiquid(tile, entity.liquids.current());
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source) {
        return item == input.item && tile.entity.items.get(input.item) < itemCapacity;
    }

    @Override
    public TileEntity getEntity() {
        return new GenericCrafterEntity();
    }
}
