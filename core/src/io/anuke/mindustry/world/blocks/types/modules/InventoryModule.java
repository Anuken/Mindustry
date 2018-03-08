package io.anuke.mindustry.world.blocks.types.modules;

import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.blocks.types.BlockModule;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class InventoryModule extends BlockModule{
    public int[] items = new int[Item.getAllItems().size];

    public int totalItems(){
        int sum = 0;
        for(int i = 0; i < items.length; i ++){
            sum += items[i];
        }
        return sum;
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
