package mindustry.world.modules;

import mindustry.type.*;

import java.io.*;
import java.util.*;

import static mindustry.Vars.content;

public class ItemModule extends BlockModule{
    private int[] items = new int[content.items().size];
    private int total;

    // Make the take() loop persistent so it does not return the same item twice in a row unless there is nothing else to return.
    protected int takeRotation;

    public void forEach(ItemConsumer cons){
        for(int i = 0; i < items.length; i++){
            if(items[i] > 0){
                cons.accept(content.item(i), items[i]);
            }
        }
    }

    public float sum(ItemCalculator calc){
        float sum = 0f;
        for(int i = 0; i < items.length; i++){
            if(items[i] > 0){
                sum += calc.get(content.item(i), items[i]);
            }
        }
        return sum;
    }

    public boolean has(Item item){
        return get(item) > 0;
    }

    public boolean has(Item item, int amount){
        return get(item) >= amount;
    }

    public boolean has(ItemStack[] stacks){
        for(ItemStack stack : stacks){
            if(!has(stack.item, stack.amount)) return false;
        }
        return true;
    }

    public boolean has(ItemStack[] stacks, float multiplier){
        for(ItemStack stack : stacks){
            if(!has(stack.item, Math.round(stack.amount * multiplier))) return false;
        }
        return true;
    }

    /**
     * Returns true if this entity has at least one of each item in each stack.
     */
    public boolean hasOne(ItemStack[] stacks){
        for(ItemStack stack : stacks){
            if(!has(stack.item, 1)) return false;
        }
        return true;
    }

    public int total(){
        return total;
    }

    public Item first(){
        for(int i = 0; i < items.length; i++){
            if(items[i] > 0){
                return content.item(i);
            }
        }
        return null;
    }

    public Item take(){
        for(int i = 0; i < items.length; i++){
            int index = (i + takeRotation);
            if(index >= items.length) index -= items.length;
            if(items[index] > 0){
                items[index] --;
                total --;
                takeRotation = index + 1;
                return content.item(index);
            }
        }
        return null;
    }

    public int get(Item item){
        return items[item.id];
    }

    public void set(Item item, int amount){
        total += (amount - items[item.id]);
        items[item.id] = amount;
    }

    public void add(Item item, int amount){
        items[item.id] += amount;
        total += amount;
    }

    public void addAll(ItemModule items){
        for(int i = 0; i < items.items.length; i++){
            this.items[i] += items.items[i];
            total += items.items[i];
        }
    }

    public void remove(Item item, int amount){
        amount = Math.min(amount, items[item.id]);

        items[item.id] -= amount;
        total -= amount;
    }

    public void remove(ItemStack stack){
        remove(stack.item, stack.amount);
    }

    public void clear(){
        Arrays.fill(items, 0);
        total = 0;
    }

    @Override
    public void write(DataOutput stream) throws IOException{
        byte amount = 0;
        for(int item : items){
            if(item > 0) amount++;
        }

        stream.writeByte(amount); //amount of items

        for(int i = 0; i < items.length; i++){
            if(items[i] > 0){
                stream.writeByte(i); //item ID
                stream.writeInt(items[i]); //item amount
            }
        }
    }

    @Override
    public void read(DataInput stream) throws IOException{
        //just in case, reset items
        Arrays.fill(items, 0);
        byte count = stream.readByte();
        total = 0;

        for(int j = 0; j < count; j++){
            int itemid = stream.readByte();
            int itemamount = stream.readInt();
            items[content.item(itemid).id] = itemamount;
            total += itemamount;
        }
    }

    public interface ItemConsumer{
        void accept(Item item, float amount);
    }

    public interface ItemCalculator{
        float get(Item item, int amount);
    }
}
