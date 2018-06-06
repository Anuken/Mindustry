package io.anuke.mindustry.world.blocks.modules;

import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.blocks.BlockModule;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class InventoryModule extends BlockModule{
    //TODO make private!
    public int[] items = new int[Item.all().size];

    public boolean hasItems(ItemStack[] stacks){
        for(ItemStack stack : stacks){
            if(!hasItem(stack.item, stack.amount)) return false;
        }
        return true;
    }

    /**Returns true if this entity has at least one of each item in each stack.*/
    public boolean hasAtLeastOneOfItems(ItemStack[] stacks){
        for(ItemStack stack : stacks){
            if(!hasItem(stack.item, 1)) return false;
        }
        return true;
    }

    //TODO optimize!
    public int totalItems(){
        int sum = 0;
        for(int i = 0; i < items.length; i ++){
            sum += items[i];
        }
        return sum;
    }

    public Item takeItem(){
        for(int i = 0; i < items.length; i ++){
            if(items[i] > 0){
                items[i] --;
                return Item.getByID(i);
            }
        }
        return null;
    }

    public int getItem(Item item){
        return items[item.id];
    }

    public boolean hasItem(Item item){
        return getItem(item) > 0;
    }

    public boolean hasItem(Item item, int amount){
        return getItem(item) >= amount;
    }

    public void addItem(Item item, int amount){
        items[item.id] += amount;
    }

    public void removeItem(Item item, int amount){
        items[item.id] -= amount;
    }

    public void removeItem(ItemStack stack){
        items[stack.item.id] -= stack.amount;
    }

    public void clear(){
        Arrays.fill(items, 0);
    }

    @Override
    public void write(DataOutputStream stream) throws IOException {
        byte amount = 0;
        for(int i = 0; i < items.length; i ++){
            if(items[i] > 0) amount ++;
        }

        stream.writeByte(amount); //amount of items

        for(int i = 0; i < items.length; i ++){
            if(items[i] > 0){
                stream.writeByte(i); //item ID
                stream.writeInt(items[i]); //item amount
            }
        }
    }

    @Override
    public void read(DataInputStream stream) throws IOException {
        byte count = stream.readByte();

        for(int j = 0; j < count; j ++){
            int itemid = stream.readByte();
            int itemamount = stream.readInt();
            items[itemid] = itemamount;
        }
    }
}
