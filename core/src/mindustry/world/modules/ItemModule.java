package mindustry.world.modules;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.type.*;

import java.util.*;

import static mindustry.Vars.*;

public class ItemModule extends BlockModule{
    public static final ItemModule empty = new ItemModule();

    private static final int windowSize = 6;
    private static WindowedMean[] cacheFlow;
    private static float[] cacheSums;
    private static float[] displayFlow;
    private static final Bits cacheBits = new Bits();
    private static final Interval flowTimer = new Interval(2);
    private static final float pollScl = 20f;

    protected int[] items = new int[content.items().size];
    protected int total;
    protected int takeRotation;

    private @Nullable WindowedMean[] flow;

    public ItemModule copy(){
        ItemModule out = new ItemModule();
        out.set(this);
        return out;
    }

    public void set(ItemModule other){
        total = other.total;
        takeRotation = other.takeRotation;
        System.arraycopy(other.items, 0, items, 0, items.length);
    }

    public void updateFlow(){
        //update the flow at N fps at most
        if(flowTimer.get(1, pollScl)){

            if(flow == null){
                if(cacheFlow == null || cacheFlow.length != items.length){
                    cacheFlow = new WindowedMean[items.length];
                    for(int i = 0; i < items.length; i++){
                        cacheFlow[i] = new WindowedMean(windowSize);
                    }
                    cacheSums = new float[items.length];
                    displayFlow = new float[items.length];
                }else{
                    for(int i = 0; i < items.length; i++){
                        cacheFlow[i].reset();
                    }
                    Arrays.fill(cacheSums, 0);
                    cacheBits.clear();
                }

                Arrays.fill(displayFlow, -1);

                flow = cacheFlow;
            }

            boolean updateFlow = flowTimer.get(30);

            for(int i = 0; i < items.length; i++){
                flow[i].add(cacheSums[i]);
                if(cacheSums[i] > 0){
                    cacheBits.set(i);
                }
                cacheSums[i] = 0;

                if(updateFlow){
                    displayFlow[i] = flow[i].hasEnoughData() ? flow[i].mean() / pollScl : -1;
                }
            }
        }
    }

    public void stopFlow(){
        flow = null;
    }

    public int length(){
        return items.length;
    }

    /** @return a specific item's flow rate in items/s; any value < 0 means not ready.*/
    public float getFlowRate(Item item){
        return flow == null ? -1f : displayFlow[item.id] * 60;
    }

    public boolean hasFlowItem(Item item){
        return flow != null && cacheBits.get(item.id);
    }

    public void each(ItemConsumer cons){
        for(int i = 0; i < items.length; i++){
            if(items[i] != 0){
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

    public boolean has(int id){
        return items[id] > 0;
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

    public boolean has(ItemSeq items){
        for(Item item : content.items()){
            if(!has(item, items.get(item))){
                return false;
            }
        }
        return true;
    }

    public boolean has(Iterable<ItemStack> stacks){
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

    public boolean any(){
        return total > 0;
    }

    public @Nullable Item first(){
        for(int i = 0; i < items.length; i++){
            if(items[i] > 0){
                return content.item(i);
            }
        }
        return null;
    }

    public @Nullable Item take(){
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

    public void add(Iterable<ItemStack> stacks){
        for(ItemStack stack : stacks){
            add(stack.item, stack.amount);
        }
    }

    public void add(ItemSeq stacks){
        stacks.each(this::add);
    }

    public void add(ItemModule items){
        for(int i = 0; i < items.items.length; i++){
            add(i, items.items[i]);
        }
    }

    public void add(Item item, int amount){
        add(item.id, amount);
    }

    private void add(int item, int amount){
        items[item] += amount;
        total += amount;
        if(flow != null){
            cacheSums[item] += amount;
        }
    }

    public void handleFlow(Item item, int amount){
        if(flow != null){
            cacheSums[item.id] += amount;
        }
    }

    public void undoFlow(Item item){
        if(flow != null){
            cacheSums[item.id] -= 1;
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

    public void remove(ItemSeq stacks){
        stacks.each(this::remove);
    }

    public void remove(Iterable<ItemStack> stacks){
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
        int amount = 0;
        for(int item : items){
            if(item > 0) amount++;
        }

        write.s(amount);

        for(int i = 0; i < items.length; i++){
            if(items[i] > 0){
                write.s(i); //item ID
                write.i(items[i]); //item amount
            }
        }
    }

    @Override
    public void read(Reads read, boolean legacy){
        //just in case, reset items
        Arrays.fill(items, 0);
        int count = legacy ? read.ub() : read.s();
        total = 0;

        for(int j = 0; j < count; j++){
            int itemid = legacy ? read.ub() : read.s();
            int itemamount = read.i();
            Item item = content.item(itemid);
            if(item != null){
                items[item.id] = itemamount;
                total += itemamount;
            }
        }
    }

    public interface ItemConsumer{
        void accept(Item item, int amount);
    }

    public interface ItemCalculator{
        float get(Item item, int amount);
    }

    @Override
    public String toString(){
        var res = new StringBuilder();
        res.append("ItemModule{");
        boolean any = false;
        for(int i = 0; i < items.length; i++){
            if(items[i] != 0){
                res.append(content.items().get(i).name).append(":").append(items[i]).append(",");
                any = true;
            }
        }
        if(any){
            res.setLength(res.length() - 1);
        }
        res.append("}");
        return res.toString();
    }
}
