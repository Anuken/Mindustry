package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.production.GenericCrafter.*;

public class DrawLiquid extends DrawBlock{
    public TextureRegion liquid, top;

    public void draw(GenericCrafterBuild entity){
        Draw.rect(entity.block.region, entity.x, entity.y);

        if(entity.liquids.total() > 0.001f){
            Drawf.liquid(liquid, entity.x, entity.y,
                entity.liquids.get(((GenericCrafter)entity.block).outputLiquid.liquid) / entity.block.liquidCapacity,
                ((GenericCrafter)entity.block).outputLiquid.liquid.color);
        }

        if(top.found()) Draw.rect(top, entity.x, entity.y);
    }

    @Override
    public void load(Block block){
        top = Core.atlas.find(block.name + "-top");
        liquid = Core.atlas.find(block.name + "-liquid");
    }

    public TextureRegion[] icons(Block block){
        return top.found() ? new TextureRegion[]{block.region, top} : new TextureRegion[]{block.region};
    }
}
