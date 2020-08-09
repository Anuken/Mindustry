package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.production.GenericCrafter.*;

public class DrawMixer extends DrawBlock{
    public TextureRegion liquid, top, bottom;

    @Override
    public void draw(GenericCrafterEntity entity){
        float rotation = entity.block.rotate ? entity.rotdeg() : 0;

        Draw.rect(bottom, entity.x, entity.y, rotation);

        if(entity.liquids.total() > 0.001f){
            Draw.color(((GenericCrafter)entity.block).outputLiquid.liquid.color);
            Draw.alpha(entity.liquids.get(((GenericCrafter)entity.block).outputLiquid.liquid) / entity.block.liquidCapacity);
            Draw.rect(liquid, entity.x, entity.y, rotation);
            Draw.color();
        }

        Draw.rect(top, entity.x, entity.y, rotation);
    }

    @Override
    public void load(Block block){
        liquid = Core.atlas.find(block.name + "-liquid");
        top = Core.atlas.find(block.name + "-top");
        bottom = Core.atlas.find(block.name + "-bottom");
    }

    @Override
    public TextureRegion[] icons(Block block){
        return new TextureRegion[]{bottom, top};
    }
}
