package io.anuke.mindustry.world.blocks.production;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.production.GenericCrafter.GenericCrafterEntity;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.ucore.core.Timers;

public class PowerCrafter extends Block{
    protected final int timerDump = timers++;

    /**Optional.*/
    protected Item outputItem;
    /**Optional. Set hasLiquids to true when using.*/
    protected Liquid outputLiquid;
    protected float outputLiquidAmount;
    protected float craftTime;

    public PowerCrafter(String name) {
        super(name);
        solid = true;
        update = true;
        hasPower = true;
        hasItems = true;
    }

    @Override
    public void init() {
        super.init();

        if(outputLiquid != null){
            outputsLiquid = true;
        }
    }

    @Override
    public void setStats() {
        super.setStats();

        if(outputItem != null){
            stats.add(BlockStat.outputItem, outputItem);
        }

        if(outputLiquid != null){
            stats.add(BlockStat.liquidOutput, outputLiquid);
        }
    }

    @Override
    public void update(Tile tile) {
        GenericCrafterEntity entity = tile.entity();

        if(entity.cons.valid()){
            entity.progress += 1f/craftTime;
            entity.totalProgress += Timers.delta();
        }

        if(entity.progress >= 1f){
            entity.items.remove(consumes.item(), consumes.itemAmount());
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
    public TileEntity getEntity() {
        return new GenericCrafterEntity();
    }
}
