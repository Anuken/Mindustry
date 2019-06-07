package io.anuke.mindustry.type;

import io.anuke.arc.collection.Array;
import io.anuke.mindustry.content.Items;

public class ItemStack implements Comparable<ItemStack>{
    public Item item;
    public int amount;

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

    public boolean equals(ItemStack other){
        return other != null && other.item == item && other.amount == amount;
    }

    public static ItemStack[] with(Object... items){
        ItemStack[] stacks = new ItemStack[items.length / 2];
        for(int i = 0; i < items.length; i += 2){
            stacks[i / 2] = new ItemStack((Item)items[i], (Integer)items[i + 1]);
        }
        return stacks;
    }

    public static Array<ItemStack> list(Object... items){
        Array<ItemStack> stacks = new Array<>(items.length / 2);
        for(int i = 0; i < items.length; i += 2){
            stacks.add(new ItemStack((Item)items[i], (Integer)items[i + 1]));
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
