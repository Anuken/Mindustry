package mindustry.world.meta.values;

import arc.scene.ui.layout.Table;
import mindustry.type.ItemStack;
import mindustry.ui.ItemDisplay;
import mindustry.world.meta.StatValue;

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
