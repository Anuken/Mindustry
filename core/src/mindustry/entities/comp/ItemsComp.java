package mindustry.entities.comp;

import arc.math.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.type.*;

@Component
abstract class ItemsComp implements Posc{
    ItemStack stack = new ItemStack();
    transient float itemTime;

    abstract int itemCapacity();

    @Override
    public void update(){
        stack.amount = Mathf.clamp(stack.amount, 0, itemCapacity());
        itemTime = Mathf.lerpDelta(itemTime, Mathf.num(hasItem()), 0.05f);
    }

    Item item(){
        return stack.item;
    }

    void clearItem(){
        stack.amount = 0;
    }

    boolean acceptsItem(Item item){
        return !hasItem() || item == stack.item && stack.amount + 1 <= itemCapacity();
    }

    boolean hasItem(){
        return stack.amount > 0;
    }

    void addItem(Item item){
        addItem(item, 1);
    }

    void addItem(Item item, int amount){
        stack.amount = stack.item == item ? stack.amount + amount : amount;
        stack.item = item;
        stack.amount = Mathf.clamp(stack.amount, 0, itemCapacity());
    }

    int maxAccepted(Item item){
        return stack.item != item && stack.amount > 0 ? 0 : itemCapacity() - stack.amount;
    }
}
