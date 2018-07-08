package io.anuke.mindustry.world.consumers;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.BlockStats;
import io.anuke.mindustry.world.meta.values.ItemFilterValue;
import io.anuke.ucore.function.Predicate;

public class ConsumeItemFilter extends Consume{
    private final Predicate<Item> item;

    public ConsumeItemFilter(Predicate<Item> item) {
        this.item = item;
    }

    @Override
    public void update(Block block, TileEntity entity) {

    }

    @Override
    public boolean valid(Block block, TileEntity entity) {
        for(int i = 0; i < Item.all().size; i ++){
            Item item = Item.getByID(i);
            if(entity.items.has(item) && this.item.test(item)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void display(BlockStats stats) {
        stats.add(BlockStat.inputItems, new ItemFilterValue(item));
    }
}
