package mindustry.world.consumers;

import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import mindustry.gen.*;
import mindustry.world.meta.*;

/** Consumer class for blocks which consume power while being connected to a power graph. */
public class ConsumePower extends Consume{
    /** The maximum amount of power which can be processed per tick. This might influence efficiency or load a buffer. */
    public final float usage;
    /** The maximum power capacity in power units. */
    public final float capacity;
    /** True if the module can store power. */
    public final boolean buffered;

    public ConsumePower(float usage, float capacity, boolean buffered){
        this.usage = usage;
        this.capacity = capacity;
        this.buffered = buffered;
    }

    protected ConsumePower(){
        this(0f, 0f, false);
    }

    @Override
    public ConsumeType type(){
        return ConsumeType.power;
    }

    @Override
    public void build(Building tile, Table table){
        //No tooltip for power, for now
    }

    @Override
    public String getIcon(){
        return "icon-power";
    }

    @Override
    public void update(Building entity){
        // Nothing to do since PowerGraph directly updates entity.power.status
    }

    @Override
    public boolean valid(Building entity){
        if(buffered){
            return true;
        }else{
            return entity.power.status > 0f;
        }
    }

    @Override
    public void display(BlockStats stats){
        if(buffered){
            stats.add(BlockStat.powerCapacity, capacity, StatUnit.none);
        }else{
            stats.add(BlockStat.powerUse, usage * 60f, StatUnit.powerSecond);
        }
    }

    /**
     * Retrieves the amount of power which is requested for the given block and entity.
     * @param entity The entity which contains the power module.
     * @return The amount of power which is requested per tick.
     */
    public float requestedPower(Building entity){
        if(entity.tile().build == null) return 0f;
        if(buffered){
            return (1f-entity.power.status)*capacity;
        }else{
            try{
                return usage * Mathf.num(entity.shouldConsume());
            }catch(Exception e){
                //HACK an error will only happen with a bar that is checking its requested power, and the entity is null/a different class
                return 0;
            }
        }
    }


}
