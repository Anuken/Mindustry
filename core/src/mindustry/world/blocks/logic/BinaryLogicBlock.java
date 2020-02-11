package mindustry.world.blocks.logic;

import arc.util.ArcAnnotate.*;
import mindustry.world.*;

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
