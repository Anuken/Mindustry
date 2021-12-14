package mindustry.type;

import arc.struct.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.world.*;

public class BlockSeq{
    private ObjectIntMap<Block> blocks = new ObjectIntMap<>();

    public void add(Block block){
        blocks.increment(block);
    }

    public void add(Block block, int amount){
        blocks.increment(block, amount);
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
        short amount = read.s();
        for(int i = 0; i < amount; i++){
            blocks.put(Vars.content.block(read.s()), read.i());
        }
    }
}
