package mindustry.type;

import arc.struct.*;
import mindustry.content.*;
import mindustry.world.*;

public class BlockStack implements Comparable<BlockStack>{
    public Block block = Blocks.router;
    public int amount = 1;

    public BlockStack(Block block, int amount){
        this.block = block;
        this.amount = amount;
    }

    public BlockStack(Block block){
        this.block = block;
    }

    public BlockStack(){
    }

    public static BlockStack[] with(Object... items){
        var stacks = new BlockStack[items.length / 2];
        for(int i = 0; i < items.length; i += 2){
            stacks[i / 2] = new BlockStack((Block)items[i], ((Number)items[i + 1]).intValue());
        }
        return stacks;
    }

    public static Seq<BlockStack> list(Object... items){
        Seq<BlockStack> stacks = new Seq<>(items.length / 2);
        for(int i = 0; i < items.length; i += 2){
            stacks.add(new BlockStack((Block)items[i], ((Number)items[i + 1]).intValue()));
        }
        return stacks;
    }

    @Override
    public int compareTo(BlockStack stack){
        return block.compareTo(stack.block);
    }

    @Override
    public boolean equals(Object o){
        return this == o || (o instanceof BlockStack stack && stack.amount == amount && block == stack.block);
    }

    @Override
    public String toString(){
        return "BlockStack{" +
        "block=" + block +
        ", amount=" + amount +
        '}';
    }
}
