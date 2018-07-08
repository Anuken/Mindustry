package io.anuke.mindustry.world.blocks.production;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.LiquidBlock;
import io.anuke.mindustry.world.consumers.ConsumeLiquid;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.ucore.core.Timers;

public class LiquidMixer extends LiquidBlock{
    protected Liquid outputLiquid;
    protected float liquidPerItem = 50f;

    public LiquidMixer(String name) {
        super(name);
        hasItems = true;
        rotate = false;
        solid = true;
    }

    @Override
    public void setStats() {
        super.setStats();

        stats.add(BlockStat.liquidOutput, outputLiquid);
    }

    @Override
    public boolean shouldConsume(Tile tile){
        return tile.entity.liquids.get(outputLiquid) < liquidCapacity;
    }

    @Override
    public void update(Tile tile){
        LiquidMixerEntity entity = tile.entity();

        if(tile.entity.cons.valid()){
            float use = Math.min(consumes.get(ConsumeLiquid.class).used() * Timers.delta(), liquidCapacity - entity.liquids.get(outputLiquid));
            entity.accumulator += use;
            entity.liquids.add(outputLiquid, use);
            for (int i = 0; i < (int)(entity.accumulator / liquidPerItem); i++) {
                if(!entity.items.has(consumes.item())) break;
                entity.items.remove(consumes.item(), 1);
                entity.accumulator --;
            }
        }

        tryDumpLiquid(tile, outputLiquid);
    }

    @Override
    public TileEntity getEntity() {
        return new LiquidMixerEntity();
    }

    static class LiquidMixerEntity extends TileEntity {
        float accumulator;
    }
}
