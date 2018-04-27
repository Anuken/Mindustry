package io.anuke.mindustry.entities;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.resource.*;

public class UnitInventory {
    private Array<AmmoEntry> ammos = new Array<>();
    private int totalAmmo;
    private ItemStack item;
    private int capacity, ammoCapacity;

    public UnitInventory(int capacity, int ammoCapacity) {
        this.capacity = capacity;
        this.ammoCapacity = ammoCapacity;
    }

    public AmmoType getAmmo() {
        return ammos.size == 0 ? null : ammos.peek().type;
    }

    public boolean hasAmmo(){
        return totalAmmo > 0;
    }

    public void useAmmo(){
        AmmoEntry entry = ammos.peek();
        entry.amount --;
        if(entry.amount == 0) ammos.pop();
        totalAmmo --;
    }

    public int totalAmmo(){
        return totalAmmo;
    }

    public int ammoCapacity(){
        return ammoCapacity;
    }

    public boolean canAcceptAmmo(AmmoType type){
        return totalAmmo + type.quantityMultiplier <= ammoCapacity;
    }

    public void addAmmo(AmmoType type){
        totalAmmo += type.quantityMultiplier;

        //find ammo entry by type
        for(int i = ammos.size - 1; i >= 0; i --){
            AmmoEntry entry = ammos.get(i);

            //if found, put it to the right
            if(entry.type == type){
                entry.amount += type.quantityMultiplier;
                ammos.swap(i, ammos.size-1);
                return;
            }
        }

        //must not be found
        AmmoEntry entry = new AmmoEntry(type, (int)type.quantityMultiplier);
        ammos.add(entry);
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
        ammos.clear();
        totalAmmo = 0;
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
