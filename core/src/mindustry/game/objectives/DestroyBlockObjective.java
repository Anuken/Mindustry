package mindustry.game.objectives;

import arc.*;
import arc.math.geom.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.objectives.MapObjectives.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class DestroyBlockObjective extends MapObjective{
    public Point2 pos = new Point2();
    public Team team = Team.crux;
    public @Synthetic Block block = Blocks.router;

    public DestroyBlockObjective(Block block, int x, int y, Team team){
        this.block = block;
        this.team = team;
        this.pos.set(x, y);
    }

    public DestroyBlockObjective(){
    }

    @Override
    public boolean update(){
        var build = world.build(pos.x, pos.y);
        return build == null || build.team != team || build.block != block;
    }

    @Override
    public String text(){
        return Core.bundle.format("objective.destroyblock", block.emoji() + " ", block.localizedName);
    }

    @Override
    public void validate(){
        if(block == null) block = Blocks.router;
    }

    @Override
    public String toString(){
        return "destroyBlock: " + block + ":" + team + " " + pos;
    }
}
