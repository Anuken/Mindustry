package mindustry.world.modules;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.ArcAnnotate.*;
import arc.util.io.*;
import mindustry.type.*;

import java.util.*;

import static mindustry.Vars.content;

public class ItemModule extends BlockModule{
    private static final int windowSize = 10;

    protected int[] items = new int[content.items().size];
    protected int total;
    protected int takeRotation;

    private @Nullable WindowedMean[] flow;
    private @Nullable Bits flownIds;

    public void update(boolean showFlow){
        if(showFlow){
            if(flow == null){
                flow = new WindowedMean[items.length];
                flownIds = new Bits(items.length);
            }
        }else{
            flow = null;
            flownIds = null;
        }
    }

    /** @return a specific item's flow rate in items/s; any value < 0 means not ready.*/
    public float getFlowRate(Item item){
        if(flow == null || flow[item.id] == null || flow[item.id].getValueCount() <= 2){
            return  - 1f;
        }

        //TODO this isn't very accurate
        return 60f / ((flow[item.id].getLatest() - flow[item.id].getOldest()) / (flow[item.id].getValueCount() - 1));
    }

    public @Nullable Bits flownBits(){
        return flownIds;
    }

    public void each(ItemConsumer cons){
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

    public boolean empty(){
        return total == 0;
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

    public int get(int id){
        return items[id];
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
        if(flow != null){
            if(flow[item.id] == null) flow[item.id] = new WindowedMean(windowSize);
            flow[item.id].addValue(Time.time());
            flownIds.set(item.id);
        }
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

    public void remove(ItemStack[] stacks){
        for(ItemStack stack : stacks) remove(stack.item, stack.amount);
    }

    public void remove(ItemStack stack){
        remove(stack.item, stack.amount);
    }

    public void clear(){
        Arrays.fill(items, 0);
        total = 0;
    }

    @Override
    public void write(Writes write){
        byte amount = 0;
        for(int item : items){
            if(item > 0) amount++;
        }

        write.b(amount); //amount of items

        for(int i = 0; i < items.length; i++){
            if(items[i] > 0){
                write.b(i); //item ID
                write.i(items[i]); //item amount
            }
        }
    }

    @Override
    public void read(Reads read){
        //just in case, reset items
        Arrays.fill(items, 0);
        byte count = read.b();
        total = 0;

        for(int j = 0; j < count; j++){
            int itemid = read.b();
            int itemamount = read.i();
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
