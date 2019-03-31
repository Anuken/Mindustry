package io.anuke.mindustry.world.consumers;

import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockStats;

/**An abstract class that defines a type of resource that a block can consume.*/
public abstract class Consume{
    protected boolean optional;
    protected boolean update = true, boost = false;

    /**Apply a filter to items accepted.
     * This should set all item IDs that are present in the filter to true.*/
    public void applyItemFilter(boolean[] filter){

    }

    /**Apply a filter to liquids accepted.
     * This should set all liquid IDs that are present in the filter to true.*/
    public void applyLiquidFilter(boolean[] filter){

    }

    public Consume optional(boolean optional){
        this.optional = optional;
        return this;
    }

    public Consume update(boolean update){
        this.update = update;
        return this;
    }

    public Consume boost(boolean boost){
        this.boost = boost;
        return this;
    }

    public boolean isOptional(){
        return optional;
    }

    public boolean isUpdate(){
        return update;
    }

    public abstract ConsumeType type();

    public abstract void build(Tile tile, Table table);

    /**Called when a consumption is triggered manually.*/
    public void trigger(Block block, TileEntity entity){

    }

    public abstract String getIcon();

    public abstract void update(Block block, TileEntity entity);

    public abstract boolean valid(Block block, TileEntity entity);

    public abstract void display(BlockStats stats);
}
