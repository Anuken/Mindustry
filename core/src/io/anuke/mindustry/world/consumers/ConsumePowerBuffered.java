package io.anuke.mindustry.world.consumers;

import io.anuke.ucore.scene.ui.layout.Table;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.BlockStats;
import io.anuke.mindustry.world.meta.StatUnit;

/** Consumer class for blocks which directly consume power without buffering it. */
public class ConsumePowerBuffered extends ConsumePower{
    /** The maximum power capacity in power units. */
    public final float powerCapacity;

    /**
     * Adds a power buffer to the owner which takes ticksToFill number of ticks to be filled.
     * Note that this object does not remove power from the buffer.
     * @param powerCapacity The maximum capacity in power units.
     * @param ticksToFill   The number of ticks it shall take to fill the buffer.
     */
    public ConsumePowerBuffered(float powerCapacity, float ticksToFill){
        super(powerCapacity / ticksToFill);
        this.powerCapacity = powerCapacity;
    }

    @Override
    public boolean valid(Block block, TileEntity entity){
        // TODO - Verify: It might be necessary to know about the power required per shot/event here.
        return true;
    }

    @Override
    public void display(BlockStats stats){
        stats.add(BlockStat.powerCapacity, powerCapacity, StatUnit.powerSecond);
    }

    @Override
    public float requestedPower(Block block, TileEntity entity){
        // Only request power until the capacity is full
        return Math.max(powerPerTick, powerCapacity * (1 - entity.power.satisfaction));
    }
}
