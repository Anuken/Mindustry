package io.anuke.mindustry.world;

import io.anuke.annotations.Annotations.Struct;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.gen.BufferItem;
import io.anuke.mindustry.type.Item;

import java.io.*;

import static io.anuke.mindustry.Vars.content;

public class DirectionalItemBuffer{
    public final long[][] buffers;
    public final int[] indexes;
    private final float speed;

    public DirectionalItemBuffer(int capacity, float speed){
        this.buffers = new long[4][capacity];
        this.indexes = new int[5];
        this.speed = speed;
    }

    public boolean accepts(int buffer){
        return indexes[buffer] < buffers[buffer].length;
    }

    public void accept(int buffer, Item item){
        if(!accepts(buffer)) return;
        buffers[buffer][indexes[buffer]++] = BufferItem.get((byte)item.id, Time.time());
    }

    public Item poll(int buffer){
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

    public void write(DataOutput stream) throws IOException{
        for(int i = 0; i < 4; i++){
            stream.writeByte(indexes[i]);
            stream.writeByte(buffers[i].length);
            for(long l : buffers[i]){
                stream.writeLong(l);
            }
        }
    }

    public void read(DataInput stream) throws IOException{
        for(int i = 0; i < 4; i++){
            indexes[i] = stream.readByte();
            byte length = stream.readByte();
            for(int j = 0; j < length; j++){
                long value = stream.readLong();
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
