package mindustry.world.consumers;

import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.meta.*;

/** Consumer class for blocks which consume power while being connected to a power graph. */
public class ConsumePower extends Consume{
    /** The maximum amount of power which can be processed per tick. This might influence efficiency or load a buffer. */
    public float usage;
    /** The maximum power capacity in power units. */
    public float capacity;
    /** True if the module can store power. */
    public boolean buffered;

    public ConsumePower(float usage, float capacity, boolean buffered){
        this.usage = usage;
        this.capacity = capacity;
        this.buffered = buffered;
    }

    protected ConsumePower(){
        this(0f, 0f, false);
    }

    @Override
    public void apply(Block block){
        block.hasPower = true;
        block.consPower = this;
    }

    @Override
    public boolean ignore(){
        return buffered;
    }

    @Override
    public float efficiency(Building build){
        return build.power.status;
    }

    @Override
    public void display(Stats stats){
        if(buffered){
            stats.add(Stat.powerCapacity, capacity, StatUnit.none);
        }else{
            stats.add(Stat.powerUse, usage * 60f, StatUnit.powerSecond);
        }
    }

    /**
     * Retrieves the amount of power which is requested for the given block and entity.
     * @param entity The entity which contains the power module.
     * @return The amount of power which is requested per tick.
     */
    public float requestedPower(Building entity){
        return buffered ?
            (1f - entity.power.status) * capacity :
            usage * (entity.shouldConsume() ? 1f : 0f);
    }


}
