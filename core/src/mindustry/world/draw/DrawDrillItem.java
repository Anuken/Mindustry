package mindustry.world.draw;

import arc.Core;
import arc.graphics.g2d.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;

public class DrawDrillItem extends DrawBlock{
    TextureRegion item;

    @Override
    public void draw(Building build){
        if(!(build instanceof Drill.DrillBuild drill) || drill.dominantItem == null) return;

        Draw.color(drill.dominantItem.color);
        Draw.rect(item, build.x, build.y);
        Draw.color();
    }

    @Override
    public void load(Block block){
        item = Core.atlas.find(block.name + "-item", Core.atlas.find("drill-item-" + block.size));
    }
}
