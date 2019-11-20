package io.anuke.mindustry.world.blocks.logic;

import io.anuke.mindustry.world.*;

public class MemoryBlock extends LogicBlock{

    public MemoryBlock(String name){
        super(name);
    }

    @Override
    public int signal(Tile tile){
        return tile.<LogicEntity>entity().signal;
    }
}
