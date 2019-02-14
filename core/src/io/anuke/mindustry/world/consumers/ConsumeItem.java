package io.anuke.mindustry.world.consumers;

import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Item.Icon;
import io.anuke.mindustry.ui.ItemImage;
import io.anuke.mindustry.ui.ReqImage;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.BlockStats;

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
    public void trigger(Block block, TileEntity entity){
        entity.items.remove(item, amount);
    }

    @Override
    public void build(Tile tile, Table table){
        table.add(new ReqImage(new ItemImage(item.icon(Icon.large), amount), () -> valid(tile.block(), tile.entity))).size(8*4);
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
        stats.add(boost ? BlockStat.boostItem : BlockStat.inputItem, item);
    }
}
