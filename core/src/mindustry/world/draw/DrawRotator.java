package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.*;
import mindustry.world.*;
import mindustry.world.blocks.production.GenericCrafter.*;

public class DrawRotator extends DrawBlock{
    public TextureRegion rotator;

    @Override
    public void draw(GenericCrafterBuild entity){
        Draw.rect(entity.block.region, entity.x, entity.y);
        Draw.color(Vars.shadowColor);
        Draw.rect(rotator, entity.x - (entity.size / 2.5f), entity.y - (entity.size / 2.5f), entity.totalProgress * 2f);
        Draw.color();
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
