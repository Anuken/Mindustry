package mindustry.world.modules;

import arc.util.io.*;

/** A class that represents compartmentalized tile entity state. */
public abstract class BlockModule{
    public abstract void write(Writes write);
    public abstract void read(Reads read);
}
