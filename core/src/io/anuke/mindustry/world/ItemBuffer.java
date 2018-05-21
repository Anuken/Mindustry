package io.anuke.mindustry.world;

import com.badlogic.gdx.utils.NumberUtils;
import io.anuke.mindustry.type.Item;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Bits;

public class ItemBuffer {
    private final float speed;

    private long[] buffer;
    private int index;

    public ItemBuffer(int capacity, float speed){
        this.buffer = new long[capacity];
        this.speed = speed;
    }

    public boolean accepts(){
        return index < buffer.length;
    }

    public void accept(Item item){
        //if(!accepts()) return;
        buffer[index ++] = Bits.packLong(NumberUtils.floatToIntBits(Timers.time()), item.id);
    }

    public Item poll(){
        if(index > 0){
            long l = buffer[0];
            float time = NumberUtils.intBitsToFloat(Bits.getLeftInt(l));

            if(Timers.time() >= time + speed || Timers.time() < time){
                return Item.getByID(Bits.getRightInt(l));
            }
        }
        return null;
    }

    public void remove(){
        System.arraycopy(buffer, 1, buffer, 0, index - 1);
        index --;
    }
}
