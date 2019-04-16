package io.anuke.mindustry.world.meta.values;

import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.ui.ItemDisplay;
import io.anuke.mindustry.world.meta.StatValue;

public class ItemListValue implements StatValue{
    private final ItemStack[] stacks;

    public ItemListValue(ItemStack... stacks){
        this.stacks = stacks;
    }

    @Override
    public void display(Table table){
        for(ItemStack stack : stacks){
            table.add(new ItemDisplay(stack.item, stack.amount)).padRight(5);
        }
    }
}
