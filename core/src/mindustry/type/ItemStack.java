package mindustry.type;

import arc.struct.Seq;
import mindustry.content.Items;

public class ItemStack implements Comparable<ItemStack>{
    public static final ItemStack[] empty = {};

    public Item item;
    public int amount = 0;

    public ItemStack(Item item, int amount){
        if(item == null) item = Items.copper;
        this.item = item;
        this.amount = amount;
    }

    //serialization only
    public ItemStack(){
        //prevent nulls.
        item = Items.copper;
    }

    public ItemStack set(Item item, int amount){
        this.item = item;
        this.amount = amount;
        return this;
    }

    public ItemStack copy(){
        return new ItemStack(item, amount);
    }

    public boolean equals(ItemStack other){
        return other != null && other.item == item && other.amount == amount;
    }

    public static ItemStack[] mult(ItemStack[] stacks, int amount){
        ItemStack[] copy = new ItemStack[stacks.length];
        for(int i = 0; i < copy.length; i++){
            copy[i] = new ItemStack(stacks[i].item, stacks[i].amount * amount);
        }
        return copy;
    }

    public static ItemStack[] with(Object... items){
        ItemStack[] stacks = new ItemStack[items.length / 2];
        for(int i = 0; i < items.length; i += 2){
            stacks[i / 2] = new ItemStack((Item)items[i], ((Number)items[i + 1]).intValue());
        }
        return stacks;
    }

    public static Seq<ItemStack> list(Object... items){
        Seq<ItemStack> stacks = new Seq<>(items.length / 2);
        for(int i = 0; i < items.length; i += 2){
            stacks.add(new ItemStack((Item)items[i], ((Number)items[i + 1]).intValue()));
        }
        return stacks;
    }

    @Override
    public int compareTo(ItemStack itemStack){
        return item.compareTo(itemStack.item);
    }

    @Override
    public String toString(){
        return "ItemStack{" +
        "item=" + item +
        ", amount=" + amount +
        '}';
    }
}
