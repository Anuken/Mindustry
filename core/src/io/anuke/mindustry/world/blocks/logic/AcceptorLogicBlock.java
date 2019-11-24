package io.anuke.mindustry.world.blocks.logic;

import io.anuke.mindustry.world.*;

public class AcceptorLogicBlock extends LogicBlock{

    public AcceptorLogicBlock(String name){
        super(name);
        doOutput = false;
    }

    @Override
    public int signal(Tile tile){
        int max = 0;
        for(Tile other : tile.entity.proximity()){
            if(rotate && tile.front() == other) continue;
            max |= getSignal(tile, other);
        }
        return max;
    }
}
