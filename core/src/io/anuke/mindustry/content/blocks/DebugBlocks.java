package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerBlock;
import io.anuke.mindustry.world.blocks.types.distribution.Sorter;
import io.anuke.mindustry.world.blocks.types.power.PowerDistributor;

public class DebugBlocks {
    public static final Block

    powerVoid = new PowerBlock("powervoid") {
        {
            powerCapacity = Float.MAX_VALUE;
        }
    },

    powerInfinite = new PowerDistributor("powerinfinite") {
        {
            powerCapacity = 10000f;
        }

        @Override
        public void update(Tile tile){
            super.update(tile);
            tile.entity.power.amount = powerCapacity;
        }
    },

    itemSource = new Sorter("itemsource"){
        @Override
        public void update(Tile tile) {
            SorterEntity entity = tile.entity();
            entity.inventory.items[entity.sortItem.id] = 1;
            tryDump(tile, entity.sortItem);
        }

        @Override
        public boolean acceptItem(Item item, Tile tile, Tile source){
            return false;
        }
    },

    itemVoid = new Block("itemvoid"){
        {
            update = solid = true;
        }

        @Override
        public void handleItem(Item item, Tile tile, Tile source) {}

        @Override
        public boolean acceptItem(Item item, Tile tile, Tile source){
            return true;
        }
    };
}
