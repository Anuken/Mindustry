package mindustry.world.blocks.logic;

import arc.util.ArcAnnotate.*;
import mindustry.world.*;

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
