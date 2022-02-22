package mindustry.world.consumers;

import arc.scene.ui.layout.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.meta.*;

/** An abstract class that defines a type of resource that a block can consume. */
public abstract class Consume{

    //TODO maybe remove these and make it an interface if possible?
    /** If true, this consumer will not influence consumer validity. */
    public boolean optional;
    /** If true, this consumer will be displayed as a boost input. */
    public boolean booster;
    //TODO bad. I don't like it.
    @Deprecated
    public boolean update = true;

    /**
     * Apply extra filters to a block.
     */
    public void apply(Block block){

    }

    public Consume optional(boolean optional, boolean boost){
        this.optional = optional;
        this.booster = boost;
        return this;
    }

    public Consume boost(){
        return optional(true, true);
    }

    @Deprecated
    public Consume update(boolean update){
        this.update = update;
        return this;
    }

    /** @return if true, this consumer will be ignored in the consumer list (no updates or valid() checks) */
    public boolean ignore(){
        return false;
    }

    public void build(Building build, Table table){}

    /** Called when a consumption is triggered manually. */
    public void trigger(Building build){}

    public void update(Building build){}

    /** @return efficiency multiplier based on input; overridden in subclasses. Returns 0 if not valid in subclasses. Should return fraction if needs are partially met. */
    public float efficiency(Building build){
        return 1f;
    }

    public void display(Stats stats){}

    //TODO this should use efficiency instead - remove or deprecate
    //public abstract boolean valid(Building build);
}
