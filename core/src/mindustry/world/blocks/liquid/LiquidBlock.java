package mindustry.world.blocks.liquid;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class LiquidBlock extends Block{
    protected TextureRegion liquidRegion, bottomRegion, topRegion;

    public LiquidBlock(String name){
        super(name);
        update = true;
        solid = true;
        hasLiquids = true;
        group = BlockGroup.liquids;
        outputsLiquid = true;
    }

    @Override
    public void load(){
        super.load();

        liquidRegion = Core.atlas.find(name + "-liquid");
        topRegion = Core.atlas.find(name + "-top");
        bottomRegion = Core.atlas.find(name + "-bottom");
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name + "-bottom"), Core.atlas.find(name + "-top")};
    }

    public class LiquidBlockEntity extends TileEntity{
        @Override
        public void draw(){
            int rotation = rotate ? rotation() * 90 : 0;
            Draw.rect(bottomRegion, x, y, rotation);

            if(liquids.total() > 0.001f){
                Draw.color(liquids.current().color);
                Draw.alpha(liquids.total() / liquidCapacity);
                Draw.rect(liquidRegion, x, y, rotation);
                Draw.color();
            }

            Draw.rect(topRegion, x, y, rotation);
        }
    }
}
