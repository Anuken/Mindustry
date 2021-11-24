package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;

/** Not standalone. */
public class DrawRegion extends DrawBlock{
    public TextureRegion region;
    public String suffix = "";
    public float rotateSpeed, x, y;
    /** Any number <=0 disables layer changes. */
    public float layer = -1;

    public DrawRegion(String suffix){
        this.suffix = suffix;
    }

    public DrawRegion(){
    }

    @Override
    public void drawBase(Building build){
        float z = Draw.z();
        if(layer > 0) Draw.z(layer);
        Draw.rect(region, build.x + x, build.y + y, build.totalProgress() * rotateSpeed);
        Draw.z(z);
    }

    @Override
    public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(region, plan.drawx(), plan.drawy());
    }

    @Override
    public void load(Block block){
        region = Core.atlas.find(block.name + suffix);
    }
}
