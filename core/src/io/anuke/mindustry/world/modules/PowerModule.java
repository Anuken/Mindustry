package io.anuke.mindustry.world.modules;

import com.badlogic.gdx.utils.IntArray;
import io.anuke.mindustry.world.blocks.power.PowerGraph;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PowerModule extends BlockModule{
    public float amount;
    public PowerGraph graph = new PowerGraph();
    public IntArray links = new IntArray();

    @Override
    public void write(DataOutput stream) throws IOException{
        stream.writeFloat(amount);

        stream.writeShort(links.size);
        for(int i = 0; i < links.size; i++){
            stream.writeInt(links.get(i));
        }
    }

    @Override
    public void read(DataInput stream) throws IOException{
        amount = stream.readFloat();
        if(Float.isNaN(amount)){
            amount = 0f;
        }
        // Workaround: If power went negative for some reason, at least fix it when reloading the map
        if(amount < 0f){
            amount = 0f;
        }

        short amount = stream.readShort();
        for(int i = 0; i < amount; i++){
            links.add(stream.readInt());
        }
    }
}
