package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.production.Pump.*;

public class DrawPumpLiquid extends DrawBlock{
    public TextureRegion liquid;

    @Override
    public void draw(Building build){
        if(!(build instanceof PumpBuild pump) || pump.liquidDrop == null) return;

        Drawf.liquid(liquid, build.x, build.y, build.liquids.get(pump.liquidDrop) / build.block.liquidCapacity, pump.liquidDrop.color);
    }

    @Override
    public void load(Block block){
        liquid = Core.atlas.find(block.name + "-liquid");
    }
}
