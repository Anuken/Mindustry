package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;

/** Not standalone. */
public class DrawLiquidRegion extends DrawPartial{
    public Liquid drawLiquid;
    public TextureRegion liquid;
    public String suffix = "-liquid";
    public float alpha = 1f;

    public DrawLiquidRegion(Liquid drawLiquid){
        this.drawLiquid = drawLiquid;
    }

    public DrawLiquidRegion(){
    }

    @Override
    public void drawBase(Building build){
        if(!build.block.hasLiquids) return;

        Liquid drawn = drawLiquid != null ? drawLiquid : build.liquids.current();
        Drawf.liquid(liquid, build.x, build.y,
            build.liquids.get(drawn) / build.block.liquidCapacity,
            Tmp.c1.set(drawn.color).a(drawn.color.a * alpha)
        );
    }

    @Override
    public void load(Block block){
        liquid = Core.atlas.find(block.name + suffix);
    }
}
