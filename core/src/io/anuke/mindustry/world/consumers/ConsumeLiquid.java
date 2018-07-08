package io.anuke.mindustry.world.consumers;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.BlockStats;
import io.anuke.mindustry.world.meta.StatUnit;
import io.anuke.ucore.core.Timers;

public class ConsumeLiquid extends Consume {
    protected final float use;
    protected final Liquid liquid;

    public ConsumeLiquid(Liquid liquid, float use) {
        this.liquid = liquid;
        this.use = use;
    }

    public float used() {
        return use;
    }

    public Liquid get() {
        return liquid;
    }

    @Override
    public void update(Block block, TileEntity entity) {
        entity.liquids.remove(liquid, Math.min(use(block), entity.liquids.get(liquid)));
    }

    @Override
    public boolean valid(Block block, TileEntity entity) {
        return entity.liquids.get(liquid) >= use(block);
    }

    @Override
    public void display(BlockStats stats) {
        stats.add(BlockStat.liquidUse, use * 60f, StatUnit.liquidSecond);
        stats.add(BlockStat.inputLiquid, liquid);
    }

    float use(Block block) {
        return Math.min(use * Timers.delta(), block.liquidCapacity);
    }
}
