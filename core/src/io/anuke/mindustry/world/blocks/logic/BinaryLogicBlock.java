package io.anuke.mindustry.world.blocks.logic;

import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.world.*;

public abstract class BinaryLogicBlock extends LogicBlock{
    protected @NonNull BinaryProcessor processor;

    public BinaryLogicBlock(String name){
        super(name);
    }

    @Override
    public byte signal(Tile tile){
        return (byte)processor.process(sleft(tile), sright(tile));
    }

    public interface BinaryProcessor{
        int process(byte left, byte right);
    }
}
