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
        Draw.rect(build.block.region, build.x, build.y);
        if(drawSpinSprite){
            Drawf.spinSprite(rotator, build.x, build.y, build.totalProgress * spinSpeed);
        }else{
            Draw.rect(rotator, build.x, build.y, build.totalProgress * spinSpeed);
        }
        if(top.found()) Draw.rect(top, build.x, build.y);
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
