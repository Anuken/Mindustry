package io.anuke.mindustry.type;

public class ItemStack{
    public io.anuke.mindustry.type.Item item;
    public int amount;

    public ItemStack(io.anuke.mindustry.type.Item item, int amount){
        this.item = item;
        this.amount = amount;
    }

    public boolean equals(ItemStack other){
        return other != null && other.item == item && other.amount == amount;
    }
}
