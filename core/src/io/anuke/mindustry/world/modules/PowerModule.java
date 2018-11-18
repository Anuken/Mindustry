package io.anuke.mindustry.world.modules;

import com.badlogic.gdx.utils.IntArray;
import io.anuke.mindustry.world.blocks.power.PowerGraph;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PowerModule extends BlockModule{
    public float satisfaction;
    public float extraUse = 0f;
    public PowerGraph graph = new PowerGraph();
    public IntArray links = new IntArray();

    @Override
    public void write(DataOutput stream) throws IOException{
        stream.writeShort(links.size);
        for(int i = 0; i < links.size; i++){
            stream.writeInt(links.get(i));
        }
    }

    @Override
    public void read(DataInput stream) throws IOException{
        short amount = stream.readShort();
        for(int i = 0; i < amount; i++){
            links.add(stream.readInt());
        }
    }
}
