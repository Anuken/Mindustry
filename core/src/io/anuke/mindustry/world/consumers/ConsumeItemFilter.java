package io.anuke.mindustry.world.consumers;

import io.anuke.arc.function.Predicate;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Item.Icon;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.BlockStats;
import io.anuke.mindustry.world.meta.values.ItemFilterValue;

import static io.anuke.mindustry.Vars.content;

public class ConsumeItemFilter extends Consume{
    public final Predicate<Item> filter;

    public ConsumeItemFilter(Predicate<Item> item){
        this.filter = item;
    }

    @Override
    public void applyItemFilter(boolean[] arr){
        content.items().each(filter, item -> arr[item.id] = true);
    }

    @Override
    public ConsumeType type(){
        return ConsumeType.item;
    }

    @Override
    public void build(Tile tile, Table table){
        MultiReqImage image = new MultiReqImage();
        content.items().each(filter, item -> image.add(new ReqImage(new ItemImage(item.icon(Icon.large), 1), () -> tile.entity != null && tile.entity.items != null && tile.entity.items.has(item))));

        table.add(image).size(8 * 4);
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
        for(int i = 0; i < content.items().size; i++){
            Item item = content.item(i);
            if(entity.items != null && entity.items.has(item) && this.filter.test(item)){
                entity.items.remove(item, 1);
                break;
            }
        }
    }

    @Override
    public boolean valid(TileEntity entity){
        for(int i = 0; i < content.items().size; i++){
            Item item = content.item(i);
            if(entity.items != null && entity.items.has(item) && this.filter.test(item)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void display(BlockStats stats){
        stats.add(booster ? BlockStat.booster : BlockStat.input, new ItemFilterValue(filter));
    }
}
