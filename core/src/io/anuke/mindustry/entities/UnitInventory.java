package io.anuke.mindustry.entities;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.type.AmmoEntry;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;

import java.io.*;

public class UnitInventory {
    private Array<AmmoEntry> ammos = new Array<>();
    private int totalAmmo;
    private ItemStack item = new ItemStack(Items.stone, 0);
    //TODO move these somewhere else so they're not variables?
    private int capacity, ammoCapacity;
    private boolean infiniteAmmo;

    public UnitInventory(int capacity, int ammoCapacity) {
        this.capacity = capacity;
        this.ammoCapacity = ammoCapacity;
    }

    public boolean isFull(){
        return item != null && item.amount >= capacity;
    }

    public void setInfiniteAmmo(boolean infinite){
        infiniteAmmo = infinite;
    }

    public void write(DataOutput stream) throws IOException {
        stream.writeShort(item.amount);
        stream.writeByte(item.item.id);
        stream.writeBoolean(infiniteAmmo);
        stream.writeShort(totalAmmo);
        stream.writeByte(ammos.size);
        for(int i = 0; i < ammos.size; i ++){
            stream.writeByte(ammos.get(i).type.id);
            stream.writeShort(ammos.get(i).amount);
        }
    }

    public void read(DataInput stream) throws IOException {
        short iamount = stream.readShort();
        byte iid = stream.readByte();
        infiniteAmmo = stream.readBoolean();
        this.totalAmmo = stream.readShort();
        byte ammoa = stream.readByte();
        for(int i = 0; i < ammoa; i ++){
            byte aid = stream.readByte();
            int am = stream.readShort();
            ammos.add(new AmmoEntry(AmmoType.getByID(aid), am));
        }

        item.item = Item.getByID(iid);
        item.amount = iamount;
    }

    /**Returns ammo range, or MAX_VALUE if this inventory has no ammo.*/
    public float getAmmoRange(){
        return hasAmmo() ? getAmmo().getRange() : Float.MAX_VALUE;
    }

    public AmmoType getAmmo() {
        return ammos.size == 0 ? null : ammos.peek().type;
    }

    public boolean hasAmmo(){
        return totalAmmo > 0;
    }

    public void useAmmo(){
        if(infiniteAmmo) return;
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
        return item.amount == 0;
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

    public boolean canAcceptItem(Item type, int amount){
        return !hasItem() || (item.item == type && item.amount + amount <= capacity);
    }

    public void clear(){
        item.amount = 0;
        ammos.clear();
        totalAmmo = 0;
    }

    public void clearItem(){
        item.amount = 0;
    }

    public boolean hasItem(){
        return item.amount > 0;
    }

    public boolean hasItem(Item i, int amount){
        return item.item == i && item.amount >= amount;
    }

    public void addItem(Item item, int amount){
        getItem().amount = getItem().item == item ? getItem().amount + amount : amount;
        getItem().item = item;
    }

    public ItemStack getItem(){
        return item;
    }
}
