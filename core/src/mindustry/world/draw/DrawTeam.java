package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class DrawTeam extends DrawBlock{
    public TextureRegion region;
    public TextureRegion[] regions;
    public String suffix = "-team";
    public boolean drawPlan = true;
    /** Any number <=0 disables layer changes. */
    public float layer = -1;

    public DrawTeam(String suffix){
        this.suffix = suffix;
    }

    public DrawTeam(){
    }

    @Override
    public void draw(Building build){
        float z = Draw.z();
        if(layer > 0) Draw.z(layer);
        if(regions[build.team.id] == region) Draw.color(build.team.color);
        Draw.rect(regions[build.team.id], build.x, build.y);
        Draw.color();
        Draw.z(z);
    }

    @Override
    public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){
        if(!drawPlan) return;
        if(plan.worldContext && player != null){
            if(regions[player.team().id] == region) Draw.color(player.team().color);
            Draw.rect(regions[player.team().id], plan.drawx(), plan.drawy());
            Draw.color();
        }
    }

    @Override
    public void load(Block block){
        if(suffix.equals("-team")){ //Block already loads team regions
            region = block.teamRegion;
            regions = block.teamRegions;
        }else{
            region = Core.atlas.find(block.name + suffix);
            regions = new TextureRegion[Team.all.length];
            for(Team team : Team.all){
                regions[team.id] = region.found() && team.hasPalette ? Core.atlas.find(block.name + suffix + "-" + team.name, region) : region;
            }
        }
    }

    @Override
    public void createIcons(Block block, MultiPacker packer){
        if(suffix.equals("-team")){ //Already handled by block
            regions = block.teamRegions;
        }else{
            regions = block.colorTeams(packer, region, suffix);
        }
    }

    @Override
    public TextureRegion[] icons(Block block){
        return new TextureRegion[]{region};
    }
}
