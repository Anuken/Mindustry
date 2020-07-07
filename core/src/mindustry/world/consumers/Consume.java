package mindustry.world.consumers;

import arc.struct.*;
import arc.scene.ui.layout.Table;
import mindustry.gen.*;
import mindustry.world.meta.BlockStats;

/** An abstract class that defines a type of resource that a block can consume. */
public abstract class Consume{
    /** If true, this consumer will not influence consumer validity. */
    protected boolean optional;
    /** If true, this consumer will be displayed as a boost input. */
    protected boolean booster;
    protected boolean update = true;

    /**
     * Apply a filter to items accepted.
     * This should set all item IDs that are present in the filter to true.
     */
    public void applyItemFilter(Bits filter){

    }

    /**
     * Apply a filter to liquids accepted.
     * This should set all liquid IDs that are present in the filter to true.
     */
    public void applyLiquidFilter(Bits filter){

    }

    public Consume optional(boolean optional, boolean boost){
        this.optional = optional;
        this.booster = boost;
        return this;
    }

    public Consume boost(){
        return optional(true, true);
    }

    public Consume update(boolean update){
        this.update = update;
        return this;
    }

    public boolean isOptional(){
        return optional;
    }

    public boolean isBoost(){
        return booster;
    }

    public boolean isUpdate(){
        return update;
    }

    public abstract ConsumeType type();

    public abstract void build(Building tile, Table table);

    /** Called when a consumption is triggered manually. */
    public void trigger(Building entity){}

    public abstract String getIcon();

    public abstract void update(Building entity);

    public abstract boolean valid(Building entity);

    public abstract void display(BlockStats stats);
}
