package io.anuke.mindustry.world.consumers;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.BlockStats;
import io.anuke.mindustry.world.meta.StatUnit;
import io.anuke.mindustry.world.meta.values.LiquidFilterValue;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.function.Predicate;

public class ConsumeLiquidFilter extends Consume{
    private final Predicate<Liquid> liquid;
    private final float use;

    public ConsumeLiquidFilter(Predicate<Liquid> liquid, float amount) {
        this.liquid = liquid;
        this.use = amount;
    }

    @Override
    public void update(Block block, TileEntity entity) {
        entity.liquids.remove(entity.liquids.current(), use(block));
    }

    @Override
    public boolean valid(Block block, TileEntity entity) {
        return liquid.test(entity.liquids.current()) && entity.liquids.currentAmount() >= use(block);
    }

    @Override
    public void display(BlockStats stats) {
        stats.add(BlockStat.inputLiquid, new LiquidFilterValue(liquid));
        stats.add(BlockStat.liquidUse, 60f * use, StatUnit.liquidSecond);
    }

    float use(Block block) {
        return Math.min(use * Timers.delta(), block.liquidCapacity);
    }
}
