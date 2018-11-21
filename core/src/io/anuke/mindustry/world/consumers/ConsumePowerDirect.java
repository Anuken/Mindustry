package io.anuke.mindustry.world.consumers;

import io.anuke.ucore.scene.ui.layout.Table;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.BlockStats;
import io.anuke.mindustry.world.meta.StatUnit;

/** Consumer class for blocks which directly consume power without buffering it. */
public class ConsumePowerDirect extends ConsumePower{
    /** The minimum power satisfaction (fraction of powerPerTick) which must be achieved before the module may work. */
    protected final float minimumSatisfaction;

    /**
     * Makes the owner consume powerPerTick each tick and disables it unless 60% of that power is being supplied.
     * @param powerPerTick The maximum amount of power which is required per tick for 100% efficiency.
     */
    public ConsumePowerDirect(float powerPerTick){
        this(powerPerTick, 0.75f);
    }

    /**
     * Makes the owner consume powerPerTick each tick and disables it unless minimumSatisfaction (1.0 = 100%) of that power is being supplied.
     * @param powerPerTick The maximum amount of power which is required per tick for 100% efficiency.
     * @param minimumSatisfaction The percentage of powerPerTick which must be available for the module to work.
     */
    public ConsumePowerDirect(float powerPerTick, float minimumSatisfaction){
        super(powerPerTick);
        this.minimumSatisfaction = minimumSatisfaction;
    }

    @Override
    public boolean valid(Block block, TileEntity entity){
        return entity.power.satisfaction >= minimumSatisfaction;
    }

    @Override
    public void display(BlockStats stats){
        stats.add(BlockStat.powerUse, powerPerTick * 60f, StatUnit.powerSecond);
    }
}
