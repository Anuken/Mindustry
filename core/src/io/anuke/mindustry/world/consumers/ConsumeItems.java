package io.anuke.mindustry.world.consumers;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.ui.ItemImage;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.BlockStats;
import io.anuke.mindustry.world.meta.values.ItemListValue;
import io.anuke.ucore.scene.ui.layout.Table;

public class ConsumeItems extends Consume{
    private ItemStack[] items;

    public ConsumeItems(ItemStack[] items){
        this.items = items;
    }

    public ItemStack[] getItems(){
        return items;
    }

    @Override
    public void buildTooltip(Table table){
        for(ItemStack stack : items){
            table.add(new ItemImage(stack)).size(8 * 4).padRight(5);
        }
    }

    @Override
    public String getIcon(){
        return "icon-item";
    }

    @Override
    public void update(Block block, TileEntity entity){

    }

    @Override
    public boolean valid(Block block, TileEntity entity){
        return entity.items != null && entity.items.has(items);
    }

    @Override
    public void display(BlockStats stats){
        stats.add(optional ? BlockStat.boostItem : BlockStat.inputItems, new ItemListValue(items));
    }
}
