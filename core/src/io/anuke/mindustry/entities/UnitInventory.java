package io.anuke.mindustry.entities;

import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.entities.traits.Saveable;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.content;

public class UnitInventory implements Saveable{
    private final Unit unit;
    private ItemStack item = new ItemStack(Items.stone, 0);

    public UnitInventory(Unit unit){
        this.unit = unit;
    }

    public boolean isFull(){
        return item != null && item.amount >= unit.getItemCapacity();
    }

    @Override
    public void writeSave(DataOutput stream) throws IOException{
        stream.writeByte(item.amount);
        stream.writeByte(item.item.id);
    }

    @Override
    public void readSave(DataInput stream) throws IOException{
        int iamount = stream.readUnsignedByte();
        byte iid = stream.readByte();

        item.item = content.item(iid);
        item.amount = iamount;
    }

    public void clear(){
        item.amount = 0;
    }

    public int capacity(){
        return unit.getItemCapacity();
    }

    public boolean isEmpty(){
        return item.amount == 0;
    }

    public int itemCapacityUsed(Item type){
        if(canAcceptItem(type)){
            return !hasItem() ? unit.getItemCapacity() : (unit.getItemCapacity() - item.amount);
        }else{
            return unit.getItemCapacity();
        }
    }

    public boolean canAcceptItem(Item type){
        return (!hasItem() && 1 <= unit.getItemCapacity()) || (item.item == type && unit.getItemCapacity() - item.amount > 0);
    }

    public boolean canAcceptItem(Item type, int amount){
        return (!hasItem() && amount <= unit.getItemCapacity()) || (item.item == type && item.amount + amount <= unit.getItemCapacity());
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
