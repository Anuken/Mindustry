package io.anuke.mindustry.world.consumers;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.meta.BlockStats;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public abstract class Consume {
    private boolean optional;

    public void optional(boolean optional) {
        this.optional = optional;
    }

    public boolean isOptional() {
        return optional;
    }

    public abstract void update(Block block, TileEntity entity);
    public abstract boolean valid(Block block, TileEntity entity);
    public abstract void display(BlockStats stats);

    public Consume copy(){ return this; }
    public void write(DataOutput stream) throws IOException{}
    public void read(DataInput stream) throws IOException{}
}
