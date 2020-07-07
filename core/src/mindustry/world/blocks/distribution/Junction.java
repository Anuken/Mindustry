package mindustry.world.blocks.distribution;

import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.content;

public class Junction extends Block{
    public float speed = 26; //frames taken to go through this junction
    public int capacity = 6;

    public Junction(String name){
        super(name);
        update = true;
        solid = true;
        group = BlockGroup.transportation;
        unloadable = false;
    }

    @Override
    public boolean outputsItems(){
        return true;
    }

    public class JunctionEntity extends Building{
        DirectionalItemBuffer buffer = new DirectionalItemBuffer(capacity);

        @Override
        public int acceptStack(Item item, int amount, Teamc source){
            return 0;
        }

        @Override
        public void updateTile(){

            for(int i = 0; i < 4; i++){
                if(buffer.indexes[i] > 0){
                    if(buffer.indexes[i] > capacity) buffer.indexes[i] = capacity;
                    long l = buffer.buffers[i][0];
                    float time = BufferItem.time(l);

                    if(Time.time() >= time + speed / timeScale || Time.time() < time){

                        Item item = content.item(BufferItem.item(l));
                        Building dest = nearby(i);

                        //skip blocks that don't want the item, keep waiting until they do
                        if(dest == null || !dest.acceptItem(this, item) || dest.team() != team){
                            continue;
                        }

                        dest.handleItem(this, item);
                        System.arraycopy(buffer.buffers[i], 1, buffer.buffers[i], 0, buffer.indexes[i] - 1);
                        buffer.indexes[i] --;
                    }
                }
            }
        }

        @Override
        public void handleItem(Building source, Item item){
            int relative = source.relativeTo(tile);
            buffer.accept(relative, item);
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            int relative = source.relativeTo(tile);

            if(relative == -1 || !buffer.accepts(relative)) return false;
            Building to = nearby(relative);
            return to != null && to.team() == team;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            buffer.write(write);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            buffer.read(read);
        }
    }
}
