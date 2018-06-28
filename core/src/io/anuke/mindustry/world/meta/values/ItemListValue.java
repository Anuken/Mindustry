package io.anuke.mindustry.world.meta.values;

import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.ui.ItemImage;
import io.anuke.mindustry.world.meta.StatValue;
import io.anuke.ucore.scene.ui.layout.Table;

public class ItemListValue implements StatValue{
    private final Item[] items;
    private final ItemStack[] stacks;

    public ItemListValue(Item[] items) {
        this.items = items;
        this.stacks = null;
    }

    public ItemListValue(ItemStack[] stacks) {
        this.stacks = stacks;
        this.items = null;
    }

    @Override
    public void display(Table table) {
        if(items != null){
            for(Item item : items){
                table.addImage(item.region).size(8*3).padRight(5);
            }
        }else{
            for(ItemStack stack : stacks){
                table.add(new ItemImage(stack)).size(8*3).padRight(5);
            }
        }
    }
}
