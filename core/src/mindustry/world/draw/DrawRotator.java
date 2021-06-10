package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.world.*;
import mindustry.world.blocks.production.GenericCrafter.*;

public class DrawRotator extends DrawBlock{
    public TextureRegion rotator, top;

    @Override
    public void draw(GenericCrafterBuild entity){
        Draw.rect(entity.block.region, entity.x, entity.y);
        Draw.rect(rotator, entity.x, entity.y, entity.totalProgress * 2f);
        if(top.found()) Draw.rect(top, entity.x, entity.y);
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
