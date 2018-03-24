package io.anuke.mindustry.world.blocks.types.storage;

import io.anuke.mindustry.resource.Item;
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
            if (tile.entity.inventory.totalItems() > 0) {
                tryDump(tile);
            }
        }
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        super.handleItem(item, tile, source);
        tile.setExtra(tile.relativeTo(source.x, source.y));
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source) {
        return tile.entity.inventory.totalItems() < itemCapacity;
    }

    @Override
    public boolean canDump(Tile tile, Tile to, Item item){
        to = to.target();
        if(!(to.block() instanceof StorageBlock)) return false;

        if(to.block() instanceof Vault){
            return (float)to.entity.inventory.totalItems() / to.block().itemCapacity <
                    (float)tile.entity.inventory.totalItems() / itemCapacity;
        }

        return true;
    }
}
