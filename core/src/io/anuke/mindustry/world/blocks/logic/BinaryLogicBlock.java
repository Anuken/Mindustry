package io.anuke.mindustry.world.blocks.logic;

import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.world.*;

public abstract class BinaryLogicBlock extends LogicBlock{
    protected @NonNull BinaryProcessor processor;

    public BinaryLogicBlock(String name){
        super(name);
    }

    @Override
    public int signal(Tile tile){
        return processor.process(sleft(tile), sright(tile));
    }

    public interface BinaryProcessor{
        int process(int left, int right);
    }
}
