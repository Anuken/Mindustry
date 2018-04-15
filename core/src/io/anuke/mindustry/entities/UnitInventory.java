package io.anuke.mindustry.entities;

import io.anuke.mindustry.resource.*;

public class UnitInventory {
    private final AmmoEntry ammo = new AmmoEntry(AmmoType.getByID(0), 0);
    private ItemStack item;
    private int capacity;

    public UnitInventory(int capacity) {
        this.capacity = capacity;
    }

    public int capacity(){
        return capacity;
    }

    public boolean isEmpty(){
        return item == null;
    }

    public int itemCapacityUsed(Item type){
        if(canAcceptItem(type)){
            return !hasItem() ? capacity : (capacity - item.amount);
        }else{
            return capacity;
        }
    }

    public boolean canAcceptItem(Item type){
        return !hasItem() || (item.item == type && capacity - item.amount > 0);
    }

    public void clear(){
        item = null;
    }

    public boolean hasItem(){
        return item != null;
    }

    public void addItem(Item item, int amount){
        if(hasItem()){
            getItem().amount = getItem().item == item ? getItem().amount + amount : amount;
            getItem().item = item;
        }else{
            this.item = new ItemStack(item, amount);
        }
    }


    public ItemStack getItem(){
        if(!hasItem()) throw new RuntimeException("This inventory has no item! Check hasItem() first.");
        return item;
    }
}
