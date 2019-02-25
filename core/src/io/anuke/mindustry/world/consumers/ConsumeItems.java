package io.anuke.mindustry.world.consumers;

import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.type.Item.Icon;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.ui.ItemImage;
import io.anuke.mindustry.ui.ReqImage;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.BlockStats;
import io.anuke.mindustry.world.meta.values.ItemListValue;

public class ConsumeItems extends Consume{
    private ItemStack[] items;

    public ConsumeItems(ItemStack[] items){
        this.items = items;
    }

    public ItemStack[] getItems(){
        return items;
    }

    @Override
    public void build(Tile tile, Table table){
        for(ItemStack stack : items){
            table.add(new ReqImage(new ItemImage(stack.item.icon(Icon.large), stack.amount), () -> valid(tile.block(), tile.entity))).size(8*4).padRight(5);
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
    public void trigger(Block block, TileEntity entity){
        for(ItemStack stack : items){
            entity.items.remove(stack);
        }
    }

    @Override
    public boolean valid(Block block, TileEntity entity){
        return entity.items != null && entity.items.has(items);
    }

    @Override
    public void display(BlockStats stats){
        stats.add(boost ? BlockStat.boostItem : BlockStat.inputItems, new ItemListValue(items));
    }
}
