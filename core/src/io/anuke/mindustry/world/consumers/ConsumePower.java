package io.anuke.mindustry.world.consumers;

import io.anuke.ucore.scene.ui.layout.Table;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.BlockStats;
import io.anuke.mindustry.world.meta.StatUnit;

/** Consumer class for blocks which consume power while being connected to a power graph. */
public abstract class ConsumePower extends Consume{
    /** The maximum amount of power which can be processed per tick. This might influence efficiency or load a buffer. */
    protected final float powerPerTick;

    public ConsumePower(float powerPerTick){
        this.powerPerTick = powerPerTick;
    }

    @Override
    public void buildTooltip(Table table){
        // No tooltip for power
    }

    @Override
    public String getIcon(){
        return "icon-power";
    }

    @Override
    public void update(Block block, TileEntity entity){
        // Nothing to do since PowerGraph directly updates entity.power.satisfaction
    }

    // valid(...) is implemented in subclass
    // display(...) is implemented in subclass

    /**
     * Retrieves the amount of power which is requested for the given block and entity.
     * @param block The block which needs power.
     * @param entity The entity which contains the power module.
     * @return The amount of power which is requested per tick.
     */
    public float requestedPower(Block block, TileEntity entity){
        // TODO Is it possible to make the block not consume power while items/liquids are missing?
        return powerPerTick;
    }
}
