package mindustry.game.objectives;

import arc.*;
import arc.math.geom.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.objectives.MapObjectives.*;
import mindustry.world.*;

import java.util.*;

import static mindustry.Vars.*;

public class DestroyBlocksObjective extends MapObjective{
    public @Unordered Point2[] positions = {};
    public Team team = Team.crux;
    public @Synthetic Block block = Blocks.router;

    public DestroyBlocksObjective(Block block, Team team, Point2... positions){
        this.block = block;
        this.team = team;
        this.positions = positions;
    }

    public DestroyBlocksObjective(){
    }

    public int progress(){
        int count = 0;
        for(var pos : positions){
            var build = world.build(pos.x, pos.y);
            if(build == null || build.team != team || build.block != block){
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean update(){
        return progress() >= positions.length;
    }

    @Override
    public String text(){
        return Core.bundle.format("objective.destroyblocks", progress(), positions.length, block.emoji() + " ", block.localizedName);
    }

    @Override
    public void validate(){
        if(block == null) block = Blocks.router;
    }

    @Override
    public String toString(){
        return "destroyBlocks: " + block + ":" + team + " " + Arrays.toString(positions);
    }
}
