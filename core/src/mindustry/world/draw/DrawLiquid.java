package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.production.GenericCrafter.*;

public class DrawLiquid extends DrawBlock{
    public TextureRegion liquid, top;

    @Override
    public void draw(GenericCrafterBuild build){
        Draw.rect(build.block.region, build.x, build.y);
        GenericCrafter type = (GenericCrafter)build.block;

        if(type.outputLiquid != null && build.liquids.get(type.outputLiquid.liquid) > 0){
            Drawf.liquid(liquid, build.x, build.y,
                build.liquids.get(type.outputLiquid.liquid) / type.liquidCapacity,
                type.outputLiquid.liquid.color
            );
        }

        if(top.found()) Draw.rect(top, build.x, build.y);
    }

    @Override
    public void load(Block block){
        top = Core.atlas.find(block.name + "-top");
        liquid = Core.atlas.find(block.name + "-liquid");
    }

    @Override
    public TextureRegion[] icons(Block block){
        return top.found() ? new TextureRegion[]{block.region, top} : new TextureRegion[]{block.region};
    }
}
