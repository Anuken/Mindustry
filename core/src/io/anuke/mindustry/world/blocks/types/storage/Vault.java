package io.anuke.mindustry.world.blocks.types.storage;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.BlockBar;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;

public class Vault extends StorageBlock {
    public int capacity = 1000;

    public Vault(String name){
        super(name);
        solid = true;
        update = true;
        bars.add(new BlockBar(Color.GREEN, true, tile -> (float)tile.entity.totalItems()/capacity));
    }

    @Override
    public void getStats(Array<String> list){
        super.getStats(list);
        list.add("[iteminfo]Capacity: " + capacity);
    }

    @Override
    public void update(Tile tile){
        int iterations = Math.max(1, (int) (Timers.delta() + 0.4f));

        for(int i = 0; i < iterations; i ++) {
            if (tile.entity.totalItems() > 0) {
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
        return tile.entity.totalItems() < capacity;
    }

    @Override
    public boolean canDump(Tile tile, Tile to, Item item){
        return to.target().block() instanceof StorageBlock;
    }
}
