package io.anuke.mindustry.world.consumers;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.util.ThreadArray;

public class Consumers{
    private ObjectMap<Class<? extends Consume>, Consume> map = new ObjectMap<>();
    private ObjectSet<Class<? extends Consume>> required = new ObjectSet<>();
    private ThreadArray<Consume> results = new ThreadArray<>();

    public void require(Class<? extends Consume> type){
        required.add(type);
    }

    public void checkRequired(Block block){
        for(Class<? extends Consume> c : required){
            if(!map.containsKey(c)){
                throw new RuntimeException("Missing required consumer of type \"" + c + "\" in block \"" + block.name + "\"!");
            }
        }

        for(Consume cons : map.values()){
            results.add(cons);
        }
    }

    public ConsumePower power(float amount){
        ConsumePower p = new ConsumePower(amount);
        add(p);
        return p;
    }

    public ConsumeLiquid liquid(Liquid liquid, float amount){
        ConsumeLiquid c = new ConsumeLiquid(liquid, amount);
        add(c);
        return c;
    }

    public ConsumeItem item(Item item){
        return item(item, 1);
    }

    public ConsumeItem item(Item item, int amount){
        ConsumeItem i = new ConsumeItem(item, amount);
        add(i);
        return i;
    }

    public ConsumeItems items(ItemStack... items){
        ConsumeItems i = new ConsumeItems(items);
        add(i);
        return i;
    }

    public Item item(){
        return get(ConsumeItem.class).get();
    }

    public ItemStack[] items(){
        return get(ConsumeItems.class).getItems();
    }

    public int itemAmount(){
        return get(ConsumeItem.class).getAmount();
    }

    public Liquid liquid(){
        return get(ConsumeLiquid.class).get();
    }

    public Consume add(Consume consume){
        map.put(consume.getClass(), consume);
        return consume;
    }

    public void remove(Class<? extends Consume> type){
        map.remove(type);
    }

    public boolean has(Class<? extends Consume> type){
        return map.containsKey(type);
    }

    public <T extends Consume> T get(Class<T> type){
        if(!map.containsKey(type)){
            throw new IllegalArgumentException("Block does not contain consumer of type '" + type + "'!");
        }
        return (T) map.get(type);
    }

    public Iterable<Consume> all(){
        return map.values();
    }

    public ThreadArray<Consume> array(){
        return results;
    }

    public boolean hasAny(){
        return map.size > 0;
    }

    public void forEach(Consumer<Consume> cons){
        for(Consume c : all()){
            cons.accept(c);
        }
    }
}
