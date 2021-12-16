package mindustry.type;

import arc.struct.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.world.*;

public class BlockSeq{
    private ObjectIntMap<Block> blocks = new ObjectIntMap<>();
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

    public void add(Block block){
        add(block, 1);
    }

    public void add(Block block, int amount){
        blocks.increment(block, amount);
        total += amount;
    }

    public void remove(Block block){
        add(block, -1);
    }

    public void remove(Block block, int amount){
        add(block, -amount);
    }

    public void remove(Seq<BlockStack> stacks){
        stacks.each(b -> remove(b.block, b.amount));
    }

    public void clear(){
        blocks.clear();
        total = 0;
    }

    public int get(Block block){
        return blocks.get(block);
    }

    public boolean contains(Seq<BlockStack> stacks){
        return !stacks.contains(b -> get(b.block) < b.amount);
    }

    public boolean contains(Block block, int amount){
        return get(block) >= amount;
    }

    public boolean contains(Block block){
        return get(block) >= 1;
    }

    public boolean contains(BlockStack stack){
        return get(stack.block) >= stack.amount;
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
        short amount = read.s();
        for(int i = 0; i < amount; i++){
            add(Vars.content.block(read.s()), read.i());
        }
    }
}
