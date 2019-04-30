package io.anuke.mindustry.world.modules;

import io.anuke.arc.collection.IntArray;
import io.anuke.mindustry.world.blocks.power.PowerGraph;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PowerModule extends BlockModule{
    /**
     * In case of unbuffered consumers, this is the percentage (1.0f = 100%) of the demanded power which can be supplied.
     * Blocks will work at a reduced efficiency if this is not equal to 1.0f.
     * In case of buffered consumers, this is the percentage of power stored in relation to the maximum capacity.
     */
    public float satisfaction = 0.0f;
    public PowerGraph graph = new PowerGraph();
    public IntArray links = new IntArray();

    @Override
    public void write(DataOutput stream) throws IOException{
        stream.writeShort(links.size);
        for(int i = 0; i < links.size; i++){
            stream.writeInt(links.get(i));
        }
        stream.writeFloat(satisfaction);
    }

    @Override
    public void read(DataInput stream) throws IOException{
        short amount = stream.readShort();
        for(int i = 0; i < amount; i++){
            links.add(stream.readInt());
        }
        satisfaction = stream.readFloat();
        if(Float.isNaN(satisfaction) || Float.isInfinite(satisfaction)) satisfaction = 0f;
    }
}
