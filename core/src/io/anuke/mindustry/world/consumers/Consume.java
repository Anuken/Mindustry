package io.anuke.mindustry.world.consumers;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.meta.BlockStats;

public abstract class Consume {
    private boolean optional;
    private boolean update;

    public Consume optional(boolean optional) {
        this.optional = optional;
        return this;
    }

    public Consume update(boolean update){
        this.update = update;
        return this;
    }

    public boolean isOptional() {
        return optional;
    }

    public boolean isUpdate() {
        return update;
    }

    public abstract void update(Block block, TileEntity entity);
    public abstract boolean valid(Block block, TileEntity entity);
    public abstract void display(BlockStats stats);
}
