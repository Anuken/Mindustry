package io.anuke.mindustry.world.blocks.logic;

import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.world.*;

public abstract class UnaryLogicBlock extends LogicBlock{
    protected @NonNull UnaryProcessor processor;

    public UnaryLogicBlock(String name){
        super(name);
    }

    @Override
    public int signal(Tile tile){
        return processor.process(sback(tile));
    }

    public interface UnaryProcessor{
        int process(int signal);
    }
}
