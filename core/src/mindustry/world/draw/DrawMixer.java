package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;

public class DrawMixer extends DrawBlock{
    public @Nullable Liquid liquidDrawn;
    public TextureRegion inLiquid, liquid, top, bottom;
    public boolean useOutputSprite;

    public DrawMixer(){
    }

    public DrawMixer(boolean useOutputSprite){
        this.useOutputSprite = useOutputSprite;
    }

    @Override
    public void drawBase(Building build){
        GenericCrafter crafter = (GenericCrafter)build.block;
        float rotation = build.block.rotate ? build.rotdeg() : 0;
        Draw.rect(bottom, build.x, build.y, rotation);

        if((inLiquid.found() || useOutputSprite) && liquidDrawn != null){
            Drawf.liquid(useOutputSprite ? liquid : inLiquid, build.x, build.y,
                build.liquids.get(liquidDrawn) / build.block.liquidCapacity,
                liquidDrawn.color
            );
        }

        if(crafter.outputLiquid != null && build.liquids.get(crafter.outputLiquid.liquid) > 0.001f){
            var liq = crafter.outputLiquid.liquid;

            Drawf.liquid(liquid, build.x, build.y, build.liquids.get(liq) / crafter.liquidCapacity, liq.color);
        }

        Draw.rect(top, build.x, build.y, rotation);
    }

    @Override
    public void load(Block block){
        expectCrafter(block);

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
