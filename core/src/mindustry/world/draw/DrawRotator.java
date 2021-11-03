package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.production.GenericCrafter.*;

public class DrawRotator extends DrawBlock{
    public TextureRegion rotator, top;
    public boolean drawSpinSprite = false;
    public float spinSpeed = 2f;

    @Override
    public void draw(GenericCrafterBuild build){
        float rot = build.drawRot();
        Draw.rect(build.block.region, build.x, build.y, rot);
        if(drawSpinSprite){
            Drawf.spinSprite(rotator, build.x, build.y, build.totalProgress * spinSpeed + rot);
        }else{
            Draw.rect(rotator, build.x, build.y, build.totalProgress * spinSpeed + rot);
        }
        if(top.found()) Draw.rect(top, build.x, build.y, rot);
    }

    @Override
    public void load(Block block){
        rotator = Core.atlas.find(block.name + "-rotator");
        top = Core.atlas.find(block.name + "-top");
    }

    @Override
    public TextureRegion[] icons(Block block){
        return top.found() ? new TextureRegion[]{block.region, rotator, top} : new TextureRegion[]{block.region, rotator};
    }
}