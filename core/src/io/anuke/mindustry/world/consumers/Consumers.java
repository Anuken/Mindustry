package io.anuke.mindustry.world.consumers;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.ucore.function.Consumer;

public class Consumers {
    private Consume[] consumeMap = new Consume[Uses.values().length];

    public ConsumePower power(float amount){
        ConsumePower p = new ConsumePower(amount);
        add(Uses.power, p);
        return p;
    }

    public ConsumeLiquid liquid(Liquid liquid, float amount){
        ConsumeLiquid c = new ConsumeLiquid(liquid, amount);
        add(Uses.liquid, c);
        return c;
    }

    public ConsumeItem item(Item item){
        ConsumeItem i = new ConsumeItem(item);
        add(Uses.items, i);
        return i;
    }

    public Item item(){
        return this.<ConsumeItem>get(Uses.items).get();
    }

    public Liquid liquid(){
        return this.<ConsumeLiquid>get(Uses.liquid).get();
    }

    public void add(Uses type, Consume consume){
        consumeMap[type.ordinal()] = consume;
    }

    public void remove(Uses type){
        consumeMap[type.ordinal()] = null;
    }

    public boolean has(Uses type){
        return consumeMap[type.ordinal()] != null;
    }

    public <T extends Consume> T get(Uses type){
        if(consumeMap[type.ordinal()] == null){
            throw new IllegalArgumentException("Block does not contain consumer of type '" + type + "'!");
        }
        return (T)consumeMap[type.ordinal()];
    }

    public void forEach(Consumer<Consume> cons){
        for (Consume c : consumeMap) {
            if (c != null) {
                cons.accept(c);
            }
        }
    }

    public void addAll(Array<Consume> result){
        for (Consume c : consumeMap) {
            if (c != null) {
                result.add(c.copy());
            }
        }
    }
}
