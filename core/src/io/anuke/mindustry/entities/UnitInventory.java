package io.anuke.mindustry.entities;

import io.anuke.mindustry.resource.*;

public class UnitInventory {
    private final AmmoEntry ammo = new AmmoEntry(AmmoType.getByID(0), 0);
    private CarryItem item;
    private int capacity;

    public UnitInventory(int capacity) {
        this.capacity = capacity;
    }

    public int capacity(){
        return capacity;
    }

    public int itemCapacityUsed(Item type){
        if(canAcceptItem(type)){
            return !hasItem() ? capacity : (capacity - getItem().amount);
        }else{
            return capacity;
        }
    }

    public boolean canAcceptItem(Item type){
        return !hasItem() || (getItem().item == type && capacity - getItem().amount > 0);
    }

    public void clear(){
        item = null;
    }

    public boolean hasAnything(){
        return item != null;
    }

    public boolean hasLiquid(){
        return item instanceof LiquidStack;
    }

    public boolean hasItem(){
        return item instanceof ItemStack;
    }

    public void addItem(Item item, int amount){
        if(hasItem()){
            getItem().amount = getItem().item == item ? getItem().amount + amount : amount;
            getItem().item = item;
        }else{
            this.item = new ItemStack(item, amount);
        }
    }

    public void addLiquid(Liquid liquid, float amount){
        if(hasItem()){
            getLiquid().liquid = liquid;
            getLiquid().amount = amount;
        }else{
            this.item = new LiquidStack(liquid, amount);
        }
    }

    public ItemStack getItem(){
        if(!hasItem()) throw new RuntimeException("This inventory has no item! Check hasItem() first.");
        return (ItemStack)item;
    }

    public LiquidStack getLiquid(){
        if(!hasItem()) throw new RuntimeException("This inventory has no item! Check hasItem() first.");
        return (LiquidStack)item;
    }
}
