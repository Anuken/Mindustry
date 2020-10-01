package mindustry.world.blocks.logic;

import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.*;
import mindustry.world.meta.*;

public class MemoryBlock extends Block{
    public int memoryCapacity = 32;
    private Runnable rebuild = null;

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

    public class MemoryBuild extends Building{
        public double[] memory = new double[memoryCapacity];

        @Override
        public void buildConfiguration(Table table){
            rebuild = () -> {
                BaseDialog dialog = new BaseDialog("Memory Contents");
                dialog.cont.table(t -> {
                    t.table(Tex.button, tt -> {
                        tt.pane(Styles.smallPane, p -> {
                            p.align(Align.left);
                            for (int i = 0; i < memory.length; i++) {
                                p.add(i + ": " + memory[i]).align(Align.left);
                                p.row();
                            }
                        }).growX().growY().margin(10f).pad(10f);
                    }).width(300f).growX().growY();

                    t.row();

                    t.button("Reload", () -> {
                        dialog.hide();
                        rebuild.run();
                    }).width(300f);
                });
                dialog.addCloseButton();
                dialog.show();
            };
            rebuild.run();
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
