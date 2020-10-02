package mindustry.world.blocks.liquid;

import arc.graphics.g2d.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class LiquidBlock extends Block{
    public @Load("@-liquid") TextureRegion liquidRegion;
    public @Load("@-top") TextureRegion topRegion;
    public @Load("@-bottom") TextureRegion bottomRegion;

    public LiquidBlock(String name){
        super(name);
        update = true;
        solid = true;
        hasLiquids = true;
        group = BlockGroup.liquids;
        outputsLiquid = true;
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{bottomRegion, topRegion};
    }

    public class LiquidBuild extends Building{
        @Override
        public void draw(){
            float rotation = rotate ? rotdeg() : 0;
            Draw.rect(bottomRegion, x, y, rotation);

            if(liquids.total() > 0.001f){
                Drawf.liquid(liquidRegion, x, y, liquids.total() / liquidCapacity, liquids.current().color);
            }

            Draw.rect(topRegion, x, y, rotation);
        }
    }
}
