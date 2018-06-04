package io.anuke.mindustry.world.blocks.types.units;

import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

public class DropPoint extends Block {

    public DropPoint(String name) {
        super(name);

        hasItems = true;
        solid = true;
        update = true;
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source) {
        return false;
    }

    @Override
    public void update(Tile tile) {
        if (tile.entity.items.totalItems() > 0) {
            tryDump(tile);
        }
    }
}
