package mindustry.world.modules;

import arc.struct.IntSeq;
import arc.util.io.*;
import mindustry.world.blocks.power.PowerGraph;

public class PowerModule extends BlockModule{
    /**
     * In case of unbuffered consumers, this is the percentage (1.0f = 100%) of the demanded power which can be supplied.
     * Blocks will work at a reduced efficiency if this is not equal to 1.0f.
     * In case of buffered consumers, this is the percentage of power stored in relation to the maximum capacity.
     */
    public float status = 0.0f;
    public PowerGraph graph = new PowerGraph();
    public IntSeq links = new IntSeq();

    @Override
    public void write(Writes write){
        write.s(links.size);
        for(int i = 0; i < links.size; i++){
            write.i(links.get(i));
        }
        write.f(status);
    }

    @Override
    public void read(Reads read){
        links.clear();
        short amount = read.s();
        for(int i = 0; i < amount; i++){
            links.add(read.i());
        }
        status = read.f();
        if(Float.isNaN(status) || Float.isInfinite(status)) status = 0f;
    }
}
