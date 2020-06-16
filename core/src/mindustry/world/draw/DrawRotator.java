package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.world.*;
import mindustry.world.blocks.production.GenericCrafter.*;

public class DrawRotator extends DrawBlock{
    public TextureRegion rotator;

    @Override
    public void draw(GenericCrafterEntity entity){
        Draw.rect(entity.block.region, entity.x, entity.y);
        Draw.rect(rotator, entity.x, entity.y, entity.totalProgress * 2f);
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
