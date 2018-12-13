package io.anuke.mindustry.world.meta.values;

import io.anuke.mindustry.game.UnlockableContent;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.ui.ItemDisplay;
import io.anuke.mindustry.world.meta.ContentStatValue;
import io.anuke.ucore.scene.ui.layout.Table;

public class ItemValue implements ContentStatValue{
    private final ItemStack item;

    public ItemValue(ItemStack item){
        this.item = item;
    }

    @Override
    public UnlockableContent[] getValueContent(){
        return new Item[]{item.item};
    }

    @Override
    public void display(Table table){
        table.add(new ItemDisplay(item.item, item.amount));
    }
}
