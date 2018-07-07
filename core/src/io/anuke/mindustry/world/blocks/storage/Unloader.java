package io.anuke.mindustry.world.blocks.storage;

import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.meta.BlockGroup;
import io.anuke.mindustry.world.Tile;

public class Unloader extends Block {
    protected final int timerUnload = timers++;
    protected int speed = 5;

    public Unloader(String name){
        super(name);
        update = true;
        solid = true;
        health = 70;
        group = BlockGroup.transportation;
        hasItems = true;
    }

    @Override
    public void update(Tile tile){
        if(tile.entity.items.total() == 0 && tile.entity.timer.get(timerUnload, speed)){
            tile.allNearby(other -> {
                if(other.block() instanceof StorageBlock && tile.entity.items.total() == 0 &&
                        ((StorageBlock)other.block()).hasItem(other, null)){
                    offloadNear(tile, ((StorageBlock)other.block()).removeItem(other, null));
                }
            });
        }

        if(tile.entity.items.total() > 0){
            tryDump(tile);
        }
    }

    @Override
    public boolean canDump(Tile tile, Tile to, Item item) {
        Block block = to.target().block();
        return !(block instanceof StorageBlock);
    }

    @Override
    public void setBars(){}
}
