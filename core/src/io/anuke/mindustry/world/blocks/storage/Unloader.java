package io.anuke.mindustry.world.blocks.storage;

import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockGroup;

public abstract class Unloader extends Block{
    protected final int timerUnload = timers++;

    public Unloader(String name){
        super(name);
        update = true;
        solid = true;
        health = 70;
        group = BlockGroup.transportation;
        hasItems = true;
    }

    @Override
    public boolean canDump(Tile tile, Tile to, Item item){
        Block block = to.target().block();
        return !(block instanceof StorageBlock);
    }

    @Override
    public void setBars(){
    }
}
