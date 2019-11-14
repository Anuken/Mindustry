package io.anuke.mindustry.world.blocks.storage;

import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;

public class CoreSeed extends CoreBlock {
    public CoreSeed(String name) {
        super(name);
        unloadable = false;
        linksToContainers = false;
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source) {
        return source.block() instanceof CoreSeed; // should accept no items from tiles, source is a coreseed if a player/drone drops off
    }

    @Override
    public boolean canBreak(Tile tile) {
        return true; // allow it to be broken, unlike actual cores
    }
}
