package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.production.GenericCrafter.*;

/** Not standalone. */
public class DrawLiquidRegion extends DrawBlock{
    public Liquid drawLiquid;
    public TextureRegion liquid;
    public String suffix = "-liquid";

    public DrawLiquidRegion(Liquid drawLiquid){
        this.drawLiquid = drawLiquid;
    }

    public DrawLiquidRegion(){
    }

    @Override
    public void drawPlan(GenericCrafter crafter, BuildPlan plan, Eachable<BuildPlan> list){}

    @Override
    public void draw(GenericCrafterBuild build){

        Liquid drawn = drawLiquid != null ? drawLiquid : build.liquids.current();
        Drawf.liquid(liquid, build.x, build.y,
            build.liquids.get(drawn) / build.block.liquidCapacity,
            drawn.color
        );
    }

    @Override
    public void load(Block block){
        liquid = Core.atlas.find(block.name + suffix);
    }
}
