package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.production.GenericCrafter.*;

public class DrawMixer extends DrawBlock{
    public TextureRegion liquid, top, bottom;

    @Override
    public void draw(GenericCrafterBuild build){
        float rotation = build.block.rotate ? build.rotdeg() : 0;

        Draw.rect(bottom, build.x, build.y, rotation);

        if(build.liquids.total() > 0.001f){
            Draw.color(((GenericCrafter)build.block).outputLiquid.liquid.color);
            Draw.alpha(build.liquids.get(((GenericCrafter)build.block).outputLiquid.liquid) / build.block.liquidCapacity);
            Draw.rect(liquid, build.x, build.y, rotation);
            Draw.color();
        }

        Draw.rect(top, build.x, build.y, rotation);
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
