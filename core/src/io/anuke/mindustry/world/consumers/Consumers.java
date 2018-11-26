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

    public ConsumeLiquid liquid(Liquid liquid, float amount){
        ConsumeLiquid c = new ConsumeLiquid(liquid, amount);
        add(c);
        return c;
    }

    /**
     * Creates a consumer which directly uses power without buffering it. The module will work while at least 60% of power is supplied.
     * @param powerPerTick The amount of power which is required each tick for 100% efficiency.
     * @return the created consumer object.
     */
    public ConsumePower powerDirect(float powerPerTick){
        return powerDirect(powerPerTick, 0.6f);
    }

    /**
     * Creates a consumer which directly uses power without buffering it. The module will work while the available power is greater than or equal to the minimumSatisfaction percentage (0..1).
     * @param powerPerTick The amount of power which is required each tick for 100% efficiency.
     * @return the created consumer object.
     */
    public ConsumePower powerDirect(float powerPerTick, float minimumSatisfaction){
        ConsumePower c = ConsumePower.consumePowerDirect(powerPerTick, minimumSatisfaction);
        add(c);
        return c;
    }

    /**
     * Creates a consumer which stores power and uses it only in case of certain events (e.g. a turret firing).
     * It will take 60 ticks (one second) to fill the buffer, given enough power supplied.
     * @param powerCapacity The maximum capacity in power units.
     */
    public ConsumePower powerBuffered(float powerCapacity){
        // TODO Balance: How long should it take to fill a buffer? The lower this value, the more power will be "stolen" from direct consumers.
        return powerBuffered(powerCapacity, 60);
    }

    /**
     * Creates a consumer which stores power and uses it only in case of certain events (e.g. a turret firing).
     * @param powerCapacity The maximum capacity in power units.
     * @param ticksToFill   The number of ticks it shall take to fill the buffer.
     */
    public ConsumePower powerBuffered(float powerCapacity, float ticksToFill){
        ConsumePower c = ConsumePower.consumePowerBuffered(powerCapacity, ticksToFill);
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
