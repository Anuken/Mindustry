package mindustry.world.meta.values;

import arc.scene.ui.layout.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

public class ItemListValue implements StatValue{
    private final ItemStack[] stacks;
    private final boolean displayName;
    private final float timePeriod;

    public ItemListValue(ItemStack... stacks){
        this(true, stacks);
    }

    public ItemListValue(boolean displayName, ItemStack... stacks){
        this.stacks = stacks;
        this.displayName = displayName;
        this.timePeriod = -1f;
    }
    
    public ItemListValue(float timePeriod, ItemStack... stacks){
        this.stacks = stacks;
        this.displayName = true;
        this.timePeriod = timePeriod;
    }

    @Override
    public void display(Table table){
        for(ItemStack stack : stacks){
            if(timePeriod > 0f){
                table.add(new ItemDisplay(stack.item, stack.amount, timePeriod, displayName)).padRight(5);
            }else{
                table.add(new ItemDisplay(stack.item, stack.amount, displayName)).padRight(5);
            }
        }
    }
}
