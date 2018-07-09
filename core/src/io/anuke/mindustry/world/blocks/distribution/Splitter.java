package io.anuke.mindustry.world.blocks.distribution;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockGroup;

public class Splitter extends Block{
    protected float speed = 30f;

    public Splitter(String name){
        super(name);
        solid = true;
        instantTransfer = true;
        update = true;
        group = BlockGroup.transportation;
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        Tile to = getTileTarget(item, tile, source, false);

        return to != null;
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        Tile to = getTileTarget(item, tile, source, true);
        to.block().handleItem(item, to, tile);
    }

    Tile getTileTarget(Item item, Tile tile, Tile source, boolean flip){
        Array<Tile> proximity = tile.entity.proximity();
        int counter = tile.getDump();
        for (int i = 0; i < proximity.size; i++) {
            Tile other = proximity.get((i + counter) % proximity.size);
            if(flip) tile.setDump((byte)((tile.getDump() + 1) % proximity.size));
            if(other != source && !(source.block().instantTransfer && other.block().instantTransfer && !(other.block() instanceof Splitter)) &&
                    other.block().acceptItem(item, other, tile)){
                return other;
            }
        }
        return null;
    }
}
