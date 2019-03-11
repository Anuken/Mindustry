package io.anuke.mindustry.world.consumers;

import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.arc.function.Consumer;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Block;

public class Consumers{
    private ObjectMap<Class<? extends Consume>, Consume> map = new ObjectMap<>();
    private ObjectSet<Class<? extends Consume>> required = new ObjectSet<>();
    private Array<Consume> results = new Array<>();

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

    public ConsumeLiquid liquid(Liquid liquid, float amount){
        ConsumeLiquid c = new ConsumeLiquid(liquid, amount);
        add(c);
        return c;
    }

    /**
     * Creates a consumer which directly uses power without buffering it. The module will work while the available power is greater than or equal to the minimumSatisfaction percentage (0..1).
     * @param powerPerTick The amount of power which is required each tick for 100% efficiency.
     * @return the created consumer object.
     */
    public ConsumePower power(float powerPerTick){
        ConsumePower c = new ConsumePower(powerPerTick, 0.0f, false);
        add(c);
        return c;
    }

    /**
     * Creates a consumer which stores power and uses it only in case of certain events (e.g. a turret firing).
     * It will take 180 ticks (three second) to fill the buffer, given enough power supplied.
     * @param powerCapacity The maximum capacity in power units.
     */
    public ConsumePower powerBuffered(float powerCapacity){
        return powerBuffered(powerCapacity, 60f * 3);
    }

    /**
     * Creates a consumer which stores power and uses it only in case of certain events (e.g. a turret firing).
     * @param powerCapacity The maximum capacity in power units.
     * @param ticksToFill   The number of ticks it shall take to fill the buffer.
     */
    public ConsumePower powerBuffered(float powerCapacity, float ticksToFill){
        ConsumePower c = new ConsumePower(powerCapacity / ticksToFill, powerCapacity, true);
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

    public float liquidAmount(){
        return get(ConsumeLiquid.class).use;
    }

    public Consume add(Consume consume){
        map.put((consume instanceof ConsumePower ? ConsumePower.class : consume.getClass()), consume);
        return consume;
    }

    public void remove(Class<? extends Consume> type){
        map.remove(type);
    }

    public boolean has(Class<? extends Consume> type){
        return map.containsKey(type);
    }

    @SuppressWarnings("unchecked")
    public <T extends Consume> T get(Class<T> type){
        if(!map.containsKey(type)){
            throw new IllegalArgumentException("Block does not contain consumer of type '" + type + "'!");
        }
        return (T) map.get(type);
    }

    public Iterable<Consume> all(){
        return map.values();
    }

    public Array<Consume> array(){
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
