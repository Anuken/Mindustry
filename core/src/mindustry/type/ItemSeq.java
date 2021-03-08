package mindustry.type;

import arc.struct.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import mindustry.*;
import mindustry.io.*;
import mindustry.world.modules.*;
import mindustry.world.modules.ItemModule.*;

import java.util.*;

public class ItemSeq implements Iterable<ItemStack>, JsonSerializable{
    protected final int[] values = new int[Vars.content.items().size];
    public int total;

    public ItemSeq(){
    }

    public ItemSeq(Seq<ItemStack> stacks){
        stacks.each(this::add);
    }

    public void checkNegative(){
        for(int i = 0; i < values.length; i++){
            if(values[i] < 0) values[i] = 0;
        }
    }

    public ItemSeq copy(){
        ItemSeq out = new ItemSeq();
        out.total = total;
        System.arraycopy(values, 0, out.values, 0, values.length);
        return out;
    }

    public void each(ItemConsumer cons){
        for(int i = 0; i < values.length; i++){
            if(values[i] != 0){
                cons.accept(Vars.content.item(i), values[i]);
            }
        }
    }

    public void clear(){
        total = 0;
        Arrays.fill(values, 0);
    }

    public Seq<ItemStack> toSeq(){
        Seq<ItemStack> out = new Seq<>();
        for(int i = 0; i < values.length; i++){
            if(values[i] != 0) out.add(new ItemStack(Vars.content.item(i), values[i]));
        }
        return out;
    }

    public void min(int number){
        for(Item item : Vars.content.items()){
            set(item, Math.min(get(item), number));
        }
    }

    public boolean has(Item item){
        return values[item.id] > 0;
    }

    public boolean has(ItemSeq seq){
        for(int i = 0; i < values.length; i++){
            if(seq.values[i] > values[i]){
                return false;
            }
        }
        return true;
    }

    public boolean has(Item item, int amount){
        return values[item.id] >= amount;
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
    public void write(Json json){
        for(Item item : Vars.content.items()){
            if(values[item.id] != 0){
                json.writeValue(item.name, values[item.id]);
            }
        }
    }

    @Override
    public void read(Json json, JsonValue jsonData){
        total = 0;
        for(Item item : Vars.content.items()){
            values[item.id] = jsonData.getInt(item.name, 0);
            total += values[item.id];
        }
    }

    @Override
    public String toString(){
        return JsonIO.print(JsonIO.write(this));
    }

    @Override
    public Iterator<ItemStack> iterator(){
        return toSeq().iterator();
    }
}
