package io.anuke.mindustry.world.consumers;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.ui.ItemImage;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.BlockStats;
import io.anuke.ucore.scene.ui.layout.Table;

public class ConsumeItem extends Consume{
    private final Item item;
    private final int amount;

    public ConsumeItem(Item item){
        this.item = item;
        this.amount = 1;
    }

    public ConsumeItem(Item item, int amount){
        this.item = item;
        this.amount = amount;
    }

    public int getAmount(){
        return amount;
    }

    public Item get(){
        return item;
    }

    @Override
    public void buildTooltip(Table table){
        table.add(new ItemImage(new ItemStack(item, amount))).size(8 * 4);
    }

    @Override
    public String getIcon(){
        return "icon-item";
    }

    @Override
    public void update(Block block, TileEntity entity){
        //doesn't update because consuming items is very specific
    }

    @Override
    public boolean valid(Block block, TileEntity entity){
        return entity != null && entity.items != null && entity.items.has(item, amount);
    }

    @Override
    public void display(BlockStats stats){
        stats.add(optional ? BlockStat.boostItem : BlockStat.inputItem, item);
    }
}
