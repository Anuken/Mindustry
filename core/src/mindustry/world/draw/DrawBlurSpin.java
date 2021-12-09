package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

/** Not standalone. */
public class DrawBlurSpin extends DrawBlock{
    public TextureRegion region, blurRegion;
    public String suffix = "";
    public float rotateSpeed = 1f, x, y, blurThresh = 0.7f;

    public DrawBlurSpin(String suffix){
        this.suffix = suffix;
    }

    public DrawBlurSpin(){
    }

    @Override
    public void drawBase(Building build){
        Drawf.spinSprite(build.warmup() > blurThresh ? blurRegion : region, build.x + x, build.y + y, build.totalProgress() * rotateSpeed);
    }

    @Override
    public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(region, plan.drawx(), plan.drawy());
    }

    @Override
    public void load(Block block){
        region = Core.atlas.find(block.name + suffix);
        blurRegion = Core.atlas.find(block.name + suffix + "-blur");
    }
}
