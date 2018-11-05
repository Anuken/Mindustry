package io.anuke.mindustry.world.modules;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**A class that represents compartmentalized tile entity state.*/
public abstract class BlockModule{
    public abstract void write(DataOutput stream) throws IOException;
    public abstract void read(DataInput stream) throws IOException;
}
