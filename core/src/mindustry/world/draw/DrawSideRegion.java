package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;

public class DrawSideRegion extends DrawBlock{
    public boolean drawRegion = false;
    public TextureRegion top1, top2;

    public DrawSideRegion(){
    }

    public DrawSideRegion(boolean drawRegion){
        this.drawRegion = drawRegion;
    }

    @Override
    public void drawBase(Building build){
        if(drawRegion) Draw.rect(build.block.region, build.x, build.y);

        Draw.rect(build.rotation > 1 ? top2 : top1, build.x, build.y, build.rotdeg());
    }

    @Override
    public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){
        if(drawRegion) Draw.rect(block.region, plan.drawx(), plan.drawy());
        Draw.rect(plan.rotation > 1 ? top2 : top1, plan.drawx(), plan.drawy(), plan.rotation * 90);
    }

    @Override
    public void load(Block block){
        top1 = Core.atlas.find(block.name + "-top1");
        top2 = Core.atlas.find(block.name + "-top2");
    }

    @Override
    public TextureRegion[] icons(Block block){
        return new TextureRegion[]{block.region, top1};
    }

}
