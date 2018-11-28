package io.anuke.mindustry.world.consumers;

import io.anuke.ucore.scene.ui.layout.Table;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.BlockStats;
import io.anuke.mindustry.world.meta.StatUnit;

/** Consumer class for blocks which consume power while being connected to a power graph. */
public class ConsumePower extends Consume{
    /** The maximum amount of power which can be processed per tick. This might influence efficiency or load a buffer. */
    protected final float powerPerTick;
    /** The minimum power satisfaction (fraction of powerPerTick) which must be achieved before the module may work. */
    protected final float minimumSatisfaction;
    /** The maximum power capacity in power units. */
    public final float powerCapacity;
    /** True if the module can store power. */
    public final boolean isBuffered;

    protected ConsumePower(float powerPerTick, float minimumSatisfaction, float powerCapacity, boolean isBuffered){
        this.powerPerTick = powerPerTick;
        this.minimumSatisfaction = minimumSatisfaction;
        this.powerCapacity = powerCapacity;
        this.isBuffered = isBuffered;
    }

    /**
     * Makes the owner consume powerPerTick each tick and disables it unless minimumSatisfaction (1.0 = 100%) of that power is being supplied.
     * @param powerPerTick The maximum amount of power which is required per tick for 100% efficiency.
     * @param minimumSatisfaction The percentage of powerPerTick which must be available for the module to work.
     */
    public static ConsumePower consumePowerDirect(float powerPerTick, float minimumSatisfaction){
        return new ConsumePower(powerPerTick, minimumSatisfaction, 0.0f, false);
    }

    /**
     * Adds a power buffer to the owner which takes ticksToFill number of ticks to be filled.
     * Note that this object does not remove power from the buffer.
     * @param powerCapacity The maximum capacity in power units.
     * @param ticksToFill   The number of ticks it shall take to fill the buffer.
     */
    public static ConsumePower consumePowerBuffered(float powerCapacity, float ticksToFill){
        return new ConsumePower(powerCapacity / ticksToFill, 0.0f, powerCapacity, true);
    }

    @Override
    public void buildTooltip(Table table){
        // No tooltip for power
    }

    @Override
    public String getIcon(){
        return "icon-power-small";
    }

    @Override
    public void update(Block block, TileEntity entity){
        // Nothing to do since PowerGraph directly updates entity.power.satisfaction
    }

    @Override
    public boolean valid(Block block, TileEntity entity){
        if(isBuffered){
            // TODO - Verify: It might be necessary to know about the power required per shot/event here.
            return true;
        }else{
            return entity.power.satisfaction >= minimumSatisfaction;
        }
    }

    @Override
    public void display(BlockStats stats){
        if(isBuffered){
            stats.add(BlockStat.powerCapacity, powerCapacity, StatUnit.powerSecond);
        }else{
            stats.add(BlockStat.powerUse, powerPerTick * 60f, StatUnit.powerSecond);
        }
    }

    /**
     * Retrieves the amount of power which is requested for the given block and entity.
     * @param block The block which needs power.
     * @param entity The entity which contains the power module.
     * @return The amount of power which is requested per tick.
     */
    public float requestedPower(Block block, TileEntity entity){
        // TODO Make the block not consume power on the following conditions, either here or in PowerGraph:
        //      - Other consumers are not valid, e.g. additional input items/liquids are missing
        //      - Buffer is full
        return powerPerTick;
    }


}
