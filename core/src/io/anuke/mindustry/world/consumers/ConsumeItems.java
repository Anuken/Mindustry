package io.anuke.mindustry.world.consumers;

import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.type.Item.Icon;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.ui.ItemImage;
import io.anuke.mindustry.ui.ReqImage;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.BlockStats;
import io.anuke.mindustry.world.meta.values.ItemListValue;

public class ConsumeItems extends Consume{
    public final ItemStack[] items;

    public ConsumeItems(ItemStack[] items){
        this.items = items;
    }

    @Override
    public void applyItemFilter(boolean[] filter){
        for(ItemStack stack : items){
            filter[stack.item.id] = true;
        }
    }

    @Override
    public ConsumeType type(){
        return ConsumeType.item;
    }

    @Override
    public void build(Tile tile, Table table){
        for(ItemStack stack : items){
            table.add(new ReqImage(new ItemImage(stack.item.icon(Icon.large), stack.amount), () -> tile.entity != null && tile.entity.items != null && tile.entity.items.has(stack.item, stack.amount))).size(8 * 4).padRight(5);
        }
    }

    @Override
    public String getIcon(){
        return "icon-item";
    }

    @Override
    public void update(TileEntity entity){

    }

    @Override
    public void trigger(TileEntity entity){
        for(ItemStack stack : items){
            entity.items.remove(stack);
        }
    }

    @Override
    public boolean valid(TileEntity entity){
        return entity.items != null && entity.items.has(items);
    }

    @Override
    public void display(BlockStats stats){
        stats.add(booster ? BlockStat.booster : BlockStat.input, new ItemListValue(items));
    }
}
