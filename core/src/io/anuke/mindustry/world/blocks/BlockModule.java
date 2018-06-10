package io.anuke.mindustry.world.blocks;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public abstract class BlockModule {
    public abstract void write(DataOutput stream) throws IOException;
    public abstract void read(DataInput stream) throws IOException;
}
