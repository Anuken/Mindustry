package io.anuke.mindustry.world.blocks.types.storage;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.BlockBar;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;

public class Vault extends Block {
    public int capacity;

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
            if (tile.entity.totalItems() > 0) { //TODO only output to the right blocks
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

    boolean canOutput(Tile tile, Tile to){
        return to != null && (to.block() instanceof Vault || to.block() instanceof CoreBlock);
    }
}
