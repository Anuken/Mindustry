package io.anuke.mindustry.world.consumers;

import io.anuke.arc.math.Mathf;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.BlockStats;
import io.anuke.mindustry.world.meta.StatUnit;

/** Consumer class for blocks which consume power while being connected to a power graph. */
public class ConsumePower extends Consume{
    /** The maximum amount of power which can be processed per tick. This might influence efficiency or load a buffer. */
    protected final float powerPerTick;
    /** The maximum power capacity in power units. */
    public final float powerCapacity;
    /** True if the module can store power. */
    public final boolean isBuffered;

    public ConsumePower(float powerPerTick, float powerCapacity, boolean isBuffered){
        this.powerPerTick = powerPerTick;
        this.powerCapacity = powerCapacity;
        this.isBuffered = isBuffered;
    }

    @Override
    public void build(Tile tile, Table table){
        //table.add(new ReqImage(new Image("icon-power-requirement"), () -> valid(tile.block(), tile.entity))).size(8*4).padRight(4);
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
            return true;
        }else{
            return entity.power.satisfaction >= 0.9999f;
        }
    }

    @Override
    public void display(BlockStats stats){
        if(isBuffered){
            stats.add(BlockStat.powerCapacity, powerCapacity, StatUnit.none);
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
        if(isBuffered){
            // Stop requesting power once the buffer is full.
            return Mathf.isEqual(entity.power.satisfaction, 1.0f) ? 0.0f : powerPerTick;
        }else{
            return powerPerTick;
        }
    }


}
