package io.anuke.mindustry.world.modules;

import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import static io.anuke.mindustry.Vars.content;

public class InventoryModule extends BlockModule{
    private int[] items = new int[content.items().size];
    private int total;

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

    public boolean has(ItemStack[] stacks, float amountScaling){
        for(ItemStack stack : stacks){
            if(!has(stack.item, (int) (stack.amount * amountScaling))) return false;
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

    public Item take(){
        for(int i = 0; i < items.length; i++){
            if(items[i] > 0){
                items[i]--;
                total--;
                return content.item(i);
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
