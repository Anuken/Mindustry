package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.production.GenericCrafter.*;
import mindustry.world.consumers.*;

public class DrawMixer extends DrawBlock{
    public TextureRegion inLiquid, liquid, top, bottom;
    public boolean useOutputSprite;

    public DrawMixer(){
    }

    public DrawMixer(boolean useOutputSprite){
        this.useOutputSprite = useOutputSprite;
    }

    @Override
    public void draw(GenericCrafterBuild build){
        float rotation = build.block.rotate ? build.rotdeg() : 0;
        Draw.rect(bottom, build.x, build.y, rotation);

        if((inLiquid.found() || useOutputSprite) && build.block.consumes.has(ConsumeType.liquid)){
            Liquid input = build.block.consumes.<ConsumeLiquid>get(ConsumeType.liquid).liquid;
            Drawf.liquid(useOutputSprite ? liquid : inLiquid, build.x, build.y,
                build.liquids.get(input) / build.block.liquidCapacity,
                input.color
            );
        }

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
        inLiquid = Core.atlas.find(block.name + "-input-liquid");
        liquid = Core.atlas.find(block.name + "-liquid");
        top = Core.atlas.find(block.name + "-top");
        bottom = Core.atlas.find(block.name + "-bottom");
    }

    @Override
    public TextureRegion[] icons(Block block){
        return new TextureRegion[]{bottom, top};
    }
}
