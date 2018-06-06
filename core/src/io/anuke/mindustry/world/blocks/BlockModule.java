package io.anuke.mindustry.world.blocks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class BlockModule {
    public abstract void write(DataOutputStream stream) throws IOException;
    public abstract void read(DataInputStream stream) throws IOException;
}
