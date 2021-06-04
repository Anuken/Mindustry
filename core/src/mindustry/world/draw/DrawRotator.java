package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.world.*;
import mindustry.world.blocks.production.GenericCrafter.*;

public class DrawRotator extends DrawBlock{
    public TextureRegion rotator;

    @Override
    public void draw(GenericCrafterBuild build){
        Draw.rect(build.block.region, build.x, build.y);
        Draw.rect(rotator, build.x, build.y, build.totalProgress * 2f);
    }

    @Override
    public void load(Block block){
        rotator = Core.atlas.find(block.name + "-rotator");
    }

    @Override
    public TextureRegion[] icons(Block block){
        return new TextureRegion[]{block.region, rotator};
    }
}
