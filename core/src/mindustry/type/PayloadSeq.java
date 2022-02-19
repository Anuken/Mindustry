package mindustry.type;

import arc.struct.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.ctype.*;

public class PayloadSeq{
    private ObjectIntMap<UnlockableContent> blocks = new ObjectIntMap<>();
    private int total;

    public boolean isEmpty(){
        return total == 0;
    }

    public boolean any(){
        return total > 0;
    }

    public int total(){
        return total;
    }

    public void add(UnlockableContent block){
        add(block, 1);
    }

    public void add(UnlockableContent block, int amount){
        blocks.increment(block, amount);
        total += amount;
    }

    public void remove(UnlockableContent block){
        add(block, -1);
    }

    public void remove(UnlockableContent block, int amount){
        add(block, -amount);
    }

    public void remove(Seq<PayloadStack> stacks){
        stacks.each(b -> remove(b.item, b.amount));
    }

    public void clear(){
        blocks.clear();
        total = 0;
    }

    public int get(UnlockableContent block){
        return blocks.get(block);
    }

    public boolean contains(Seq<PayloadStack> stacks){
        return !stacks.contains(b -> get(b.item) < b.amount);
    }

    public boolean contains(UnlockableContent block, int amount){
        return get(block) >= amount;
    }

    public boolean contains(UnlockableContent block){
        return get(block) >= 1;
    }

    public boolean contains(PayloadStack stack){
        return get(stack.item) >= stack.amount;
    }

    public void write(Writes write){
        write.s(blocks.size);
        for(var entry : blocks.entries()){
            write.s(entry.key.id);
            write.i(entry.value);
        }
    }

    public void read(Reads read){
        total = 0;
        blocks.clear();
        short amount = read.s();
        for(int i = 0; i < amount; i++){
            add(Vars.content.block(read.s()), read.i());
        }
    }
}
