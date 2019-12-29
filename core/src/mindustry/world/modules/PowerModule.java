package mindustry.world.modules;

import arc.struct.IntArray;
import mindustry.world.blocks.power.PowerGraph;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PowerModule extends BlockModule{
    /**
     * In case of unbuffered consumers, this is the percentage (1.0f = 100%) of the demanded power which can be supplied.
     * Blocks will work at a reduced efficiency if this is not equal to 1.0f.
     * In case of buffered consumers, this is the percentage of power stored in relation to the maximum capacity.
     */
    public float status = 0.0f;
    public PowerGraph graph = new PowerGraph();
    public IntArray links = new IntArray();

    @Override
    public void write(DataOutput stream) throws IOException{
        stream.writeShort(links.size);
        for(int i = 0; i < links.size; i++){
            stream.writeInt(links.get(i));
        }
        stream.writeFloat(status);
    }

    @Override
    public void read(DataInput stream) throws IOException{
        links.clear();
        short amount = stream.readShort();
        for(int i = 0; i < amount; i++){
            links.add(stream.readInt());
        }
        status = stream.readFloat();
        if(Float.isNaN(status) || Float.isInfinite(status)) status = 0f;
    }
}
