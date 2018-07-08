package io.anuke.mindustry.world.consumers;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.BlockStats;
import io.anuke.mindustry.world.meta.values.ItemListValue;

public class ConsumeItems extends Consume {
    private ItemStack[] items;

    public ConsumeItems(ItemStack[] items) {
        this.items = items;
    }

    public ItemStack[] getItems() {
        return items;
    }

    @Override
    public void update(Block block, TileEntity entity) {
        for(ItemStack stack : items){
            entity.items.remove(stack);
        }
    }

    @Override
    public boolean valid(Block block, TileEntity entity) {
        return entity.items.has(items);
    }

    @Override
    public void display(BlockStats stats) {
        stats.add(BlockStat.inputItems, new ItemListValue(items));
    }
}
