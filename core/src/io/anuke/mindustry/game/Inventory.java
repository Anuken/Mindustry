package io.anuke.mindustry.game;

import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;

import java.util.Arrays;

import static io.anuke.mindustry.Vars.debug;

public class Inventory {
    private final int[] items = new int[Item.getAllItems().size];
    private boolean updated;

    public boolean isUpdated(){
        return updated;
    }

    public void setUpdated(boolean updated){
        this.updated = updated;
    }

    public void clearItems(){
        updated = true;
        Arrays.fill(items, 0);

        addItem(Item.stone, 40);

        if(debug){
            Arrays.fill(items, 99999);
        }
    }

    public void fill(){
        Arrays.fill(items, 999999999);
    }

    public int getAmount(Item item){
        return items[item.id];
    }

    public void addItem(Item item, int amount){
        updated = true;
        items[item.id] += amount;
    }

    public boolean hasItems(ItemStack[] items){
        for(ItemStack stack : items)
            if(!hasItem(stack))
                return false;
        return true;
    }

    public boolean hasItems(ItemStack[] items, int scaling){
        for(ItemStack stack : items)
            if(!hasItem(stack.item, stack.amount * scaling))
                return false;
        return true;
    }

    public boolean hasItem(ItemStack req){
        updated = true;
        return items[req.item.id] >= req.amount;
    }

    public boolean hasItem(Item item, int amount){
        updated = true;
        return items[item.id] >= amount;
    }

    public void removeItem(ItemStack req){
        updated = true;
        items[req.item.id] -= req.amount;
        if(items[req.item.id] < 0) items[req.item.id] = 0; //prevents negative item glitches in multiplayer
    }

    public void removeItems(ItemStack... reqs){
        updated = true;
        for(ItemStack req : reqs)
            removeItem(req);
    }

    public int[] getItems(){
        updated = true;
        return items;
    }
}
