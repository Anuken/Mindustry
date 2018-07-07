package io.anuke.mindustry.world.blocks.storage;

import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;

public class Vault extends StorageBlock {

    public Vault(String name){
        super(name);
        solid = true;
        update = true;
        itemCapacity = 1000;
    }

    @Override
    public void update(Tile tile){
        int iterations = Math.max(1, (int) (Timers.delta() + 0.4f));

        for(int i = 0; i < iterations; i ++) {
            if (tile.entity.items.total() > 0) {
                tryDump(tile);
            }
        }
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        super.handleItem(item, tile, source);
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source) {
        return tile.entity.items.total() < itemCapacity;
    }

    @Override
    public boolean canDump(Tile tile, Tile to, Item item) {
        to = to.target();
        if (!(to.block() instanceof StorageBlock)) return false;

        return !(to.block() instanceof Vault) || (float) to.entity.items.total() / to.block().itemCapacity < (float) tile.entity.items.total() / itemCapacity;

    }
}
