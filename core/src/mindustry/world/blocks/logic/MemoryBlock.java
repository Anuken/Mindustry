package mindustry.world.blocks.logic;

import arc.util.io.*;
import mindustry.gen.*;
import mindustry.world.*;

public class MemoryBlock extends Block{
    public int memoryCapacity = 32;

    public MemoryBlock(String name){
        super(name);
        destructible = true;
        solid = true;
    }

    public class MemoryBuild extends Building{
        public double[] memory = new double[memoryCapacity];

        @Override
        public void write(Writes write){
            super.write(write);

            write.i(memory.length);
            for(double v : memory){
                write.d(v);
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            int amount = read.i();
            memory = memory.length != amount ? new double[amount] : memory;
            for(int i = 0; i < amount; i++){
                memory[i] = read.d();
            }
        }
    }
}
