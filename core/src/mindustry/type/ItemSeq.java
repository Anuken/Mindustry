package mindustry.type;

import arc.struct.*;
import mindustry.*;
import mindustry.world.modules.*;
import mindustry.world.modules.ItemModule.*;

import java.util.*;

public class ItemSeq implements Iterable<ItemStack>{
    private final static ItemStack tmp = new ItemStack();

    protected final int[] values;
    public int total;

    public ItemSeq(){
        values = new int[Vars.content.items().size];
    }

    public void each(ItemConsumer cons){
        for(int i = 0; i < values.length; i++){
            if(values[i] > 0){
                cons.accept(Vars.content.item(i), values[i]);
            }
        }
    }

    public Seq<ItemStack> toSeq(){
        Seq<ItemStack> out = new Seq<>();
        for(int i = 0; i < values.length; i++){
            if(values[i] > 0) out.add(new ItemStack(Vars.content.item(i), values[i]));
        }
        return out;
    }

    public boolean has(Item item){
        return values[item.id] > 0;
    }

    public int get(Item item){
        return values[item.id];
    }

    public void set(Item item, int amount){
        add(item, amount - values[item.id]);
    }

    public void add(ItemModule itemModule){
        itemModule.each(this::add);
    }

    public void add(ItemSeq seq){
        seq.each(this::add);
    }

    public void add(ItemStack stack){
        add(stack.item, stack.amount);
    }

    public void add(Item item){
        add(item, 1);
    }

    public void add(Item item, int amount){
        values[item.id] += amount;
        total += amount;
    }

    public void remove(ItemStack stack){
        add(stack.item, -stack.amount);
    }

    public void remove(Item item){
        add(item, -1);
    }

    public void remove(Item item, int amount){
        add(item, -amount);
    }

    @Override
    public Iterator<ItemStack> iterator(){
        return toSeq().iterator();
    }
}
