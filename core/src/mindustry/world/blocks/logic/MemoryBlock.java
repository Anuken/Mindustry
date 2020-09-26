package mindustry.world.blocks.logic;

import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class MemoryBlock extends Block{
    public int memoryCapacity = 32;

    public MemoryBlock(String name){
        super(name);
        destructible = true;
        solid = true;
        configurable = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.memoryCapacity, memoryCapacity, StatUnit.none);
    }

    public class MemoryBuild extends Building {
        public double[] memory = new double[memoryCapacity];

        @Override
        public void buildConfiguration(Table table){
            table.table(Styles.black6, t -> {
                t.pane(p -> {
                    p.align(Align.left);
                    for (int i = 0; i < memory.length; i++) {
                        p.add(i + ": " + memory[i]).align(Align.left);
                        p.row();
                    }
                }).height(120f).growX().margin(10f).pad(10f);
            });
        }
        //massive byte size means picking up causes sync issues
        @Override
        public boolean canPickup(){
            return false;
        }

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
            for(int i = 0; i < amount; i++){
                double val = read.d();
                if(i < memory.length) memory[i] = val;
            }
        }
    }
}
