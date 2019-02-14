package io.anuke.mindustry.world.consumers;

import io.anuke.arc.collection.Array;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Item.Icon;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.BlockStats;
import io.anuke.mindustry.world.meta.values.ItemFilterValue;
import io.anuke.arc.function.Predicate;
import io.anuke.arc.scene.ui.layout.Table;
import static io.anuke.mindustry.Vars.*;

public class ConsumeItemFilter extends Consume{
    private final Predicate<Item> filter;

    public ConsumeItemFilter(Predicate<Item> item){
        this.filter = item;
    }

    @Override
    public void build(Tile tile, Table table){
        Array<Item> list = content.items().select(filter);

        for(int i = 0; i < list.size; i++){
            Item item = list.get(i);
            table.addImage(item.icon(Icon.large)).size(8 * 4).padRight(2).padLeft(2);
            if(i != list.size - 1){
                table.add("/");
            }
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
        for(int i = 0; i < content.items().size; i++){
            Item item = content.item(i);
            if(entity.items != null && entity.items.has(item) && this.filter.test(item)){
                entity.items.remove(item, 1);
                break;
            }
        }
    }

    @Override
    public boolean valid(Block block, TileEntity entity){
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
        stats.add(boost ? BlockStat.boostItem : BlockStat.inputItem, new ItemFilterValue(filter));
    }
}
