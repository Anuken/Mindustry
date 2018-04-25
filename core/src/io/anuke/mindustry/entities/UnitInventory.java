package io.anuke.mindustry.entities;

import io.anuke.mindustry.resource.*;

public class UnitInventory {
    private final AmmoEntry ammo = new AmmoEntry(AmmoType.getByID(0), 0);
    private ItemStack item;
    private int capacity, ammoCapacity;

    public UnitInventory(int capacity, int ammoCapacity) {
        this.capacity = capacity;
        this.ammoCapacity = ammoCapacity;
    }

    public AmmoType getAmmo() {
        return ammo.type;
    }

    public boolean hasAmmo(){
        return ammo.amount > 0;
    }

    public void useAmmo(){
        ammo.amount --;
    }

    public int ammoCapacity(){
        return ammoCapacity;
    }

    public boolean canAcceptAmmo(AmmoType type){
        return ammo.amount + type.quantityMultiplier <= ammoCapacity;
    }

    public void addAmmo(AmmoType type){
        if(ammo.type != type) ammo.amount = 0;
        ammo.type = type;
        ammo.amount += Math.min((int)type.quantityMultiplier, ammoCapacity - ammo.amount);
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
        ammo.amount = 0;
        ammo.type = AmmoType.getByID(0);
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
