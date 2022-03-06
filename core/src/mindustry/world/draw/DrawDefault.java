package mindustry.world.draw;

import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;

public class DrawDefault extends DrawBlock{

    @Override
    public void draw(Building build){
        Draw.rect(build.block.region, build.x, build.y, build.drawrot());
    }

    @Override
    public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){
        block.drawDefaultPlanRegion(plan, list);
    }
}
