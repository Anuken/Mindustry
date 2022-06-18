package mindustry.type;

import arc.struct.*;
import mindustry.content.*;
import mindustry.ctype.*;

public class PayloadStack implements Comparable<PayloadStack>{
    public UnlockableContent item = Blocks.router;
    public int amount = 1;

    public PayloadStack(UnlockableContent item, int amount){
        this.item = item;
        this.amount = amount;
    }

    public PayloadStack(UnlockableContent item){
        this.item = item;
    }

    public PayloadStack(){
    }

    public static PayloadStack[] with(Object... items){
        var stacks = new PayloadStack[items.length / 2];
        for(int i = 0; i < items.length; i += 2){
            stacks[i / 2] = new PayloadStack((UnlockableContent)items[i], ((Number)items[i + 1]).intValue());
        }
        return stacks;
    }

    public static Seq<PayloadStack> list(Object... items){
        Seq<PayloadStack> stacks = new Seq<>(items.length / 2);
        for(int i = 0; i < items.length; i += 2){
            stacks.add(new PayloadStack((UnlockableContent)items[i], ((Number)items[i + 1]).intValue()));
        }
        return stacks;
    }

    @Override
    public int compareTo(PayloadStack stack){
        return item.compareTo(stack.item);
    }

    @Override
    public boolean equals(Object o){
        return this == o || (o instanceof PayloadStack stack && stack.amount == amount && item == stack.item);
    }

    @Override
    public String toString(){
        return "BlockStack{" +
        "item=" + item +
        ", amount=" + amount +
        '}';
    }
}
