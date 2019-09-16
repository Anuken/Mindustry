package io.anuke.mindustry.world.meta.values;

import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.ui.ItemDisplay;
import io.anuke.mindustry.world.meta.StatValue;

public class ItemListValue implements StatValue{
    private final ItemStack[] stacks;
    private final boolean displayName;

    public ItemListValue(ItemStack... stacks){
        this(true, stacks);
    }

    public ItemListValue(boolean displayName, ItemStack... stacks){
        this.stacks = stacks;
        this.displayName = displayName;
    }

    @Override
    public void display(Table table){
        for(ItemStack stack : stacks){
            table.add(new ItemDisplay(stack.item, stack.amount, displayName)).padRight(5);
        }
    }
}
