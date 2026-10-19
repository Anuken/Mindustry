package mindustry.game.objectives;

import arc.*;
import mindustry.content.*;
import mindustry.game.objectives.MapObjectives.*;
import mindustry.world.*;

import static mindustry.Vars.*;

/** Build a certain amount of a block. */
public class BuildCountObjective extends MapObjective{
    public @Synthetic Block block = Blocks.conveyor;
    public int count = 1;

    public BuildCountObjective(Block block, int count){
        this.block = block;
        this.count = count;
    }

    public BuildCountObjective(){
    }

    @Override
    public boolean update(){
        return state.stats.placedBlockCount.get(block, 0) >= count;
    }

    @Override
    public String text(){
        return Core.bundle.format("objective.build", count - state.stats.placedBlockCount.get(block, 0), block.emoji() + " ", block.localizedName);
    }

    @Override
    public void validate(){
        if(block == null) block = Blocks.conveyor;
    }

    @Override
    public String toString(){
        return "buildCount: " + block + " x" + count;
    }
}
