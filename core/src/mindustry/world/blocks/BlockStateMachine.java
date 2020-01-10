package mindustry.world.blocks;

import mindustry.entities.units.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

public class BlockStateMachine{
    private BlockState state;

    public void update(Tile tile){
        if(state != null) state.update(tile);
    }

    public void set(Tile tile, BlockState next){
        if(next == state) return;
        if(state != null) state.exited(tile);
        this.state = next;
        if(next != null) next.entered(tile);
    }

    public BlockState current(){
        return state;
    }

    public boolean is(BlockState state){
        return this.state == state;
    }
}
