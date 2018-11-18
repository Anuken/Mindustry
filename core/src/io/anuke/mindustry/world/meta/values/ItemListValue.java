package io.anuke.mindustry.world.meta.values;

import io.anuke.mindustry.game.UnlockableContent;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.ui.ItemDisplay;
import io.anuke.mindustry.world.meta.ContentStatValue;
import io.anuke.ucore.scene.ui.layout.Table;

public class ItemListValue implements ContentStatValue{
    private final Item[] items;
    private final ItemStack[] stacks;

    public ItemListValue(Item[] items){
        this.items = items;
        this.stacks = null;
    }

    public ItemListValue(ItemStack[] stacks){
        this.stacks = stacks;
        this.items = null;
    }

    @Override
    public UnlockableContent[] getValueContent(){
        if(items != null){
            return items;
        }else{
            Item[] res = new Item[stacks.length];
            for(int i = 0; i < res.length; i++){
                res[i] = stacks[i].item;
            }
            return res;
        }
    }

    @Override
    public void display(Table table){
        if(items != null){
            for(Item item : items){
                table.add(new ItemDisplay(item));
            }
        }else{
            for(ItemStack stack : stacks){
                new ItemDisplay(stack.item, stack.amount);
            }
        }
    }
}
