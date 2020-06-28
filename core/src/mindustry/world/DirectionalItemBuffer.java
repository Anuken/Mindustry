package mindustry.world;

import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.type.*;

import static mindustry.Vars.content;

public class DirectionalItemBuffer{
    public final long[][] buffers;
    public final int[] indexes;

    public DirectionalItemBuffer(int capacity){
        this.buffers = new long[4][capacity];
        this.indexes = new int[5];
    }

    public boolean accepts(int buffer){
        return indexes[buffer] < buffers[buffer].length;
    }

    public void accept(int buffer, Item item){
        if(!accepts(buffer)) return;
        buffers[buffer][indexes[buffer]++] = BufferItem.get((byte)item.id, Time.time());
    }

    public Item poll(int buffer, float speed){
        if(indexes[buffer] > 0){
            long l = buffers[buffer][0];
            float time = BufferItem.time(l);

            if(Time.time() >= time + speed || Time.time() < time){
                return content.item(BufferItem.item(l));
            }
        }
        return null;
    }

    public void remove(int buffer){
        System.arraycopy(buffers[buffer], 1, buffers[buffer], 0, indexes[buffer] - 1);
        indexes[buffer] --;
    }

    public void write(Writes write){
        for(int i = 0; i < 4; i++){
            write.b(indexes[i]);
            write.b(buffers[i].length);
            for(long l : buffers[i]){
                write.l(l);
            }
        }
    }

    public void read(Reads read){
        for(int i = 0; i < 4; i++){
            indexes[i] = read.b();
            byte length = read.b();
            for(int j = 0; j < length; j++){
                long value = read.l();
                if(j < buffers[i].length){
                    buffers[i][j] = value;
                }
            }
        }
    }

    @Struct
    class BufferItemStruct{
        byte item;
        float time;
    }
}
