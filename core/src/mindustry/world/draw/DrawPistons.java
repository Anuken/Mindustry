package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;

public class DrawPistons extends DrawBlock{
    public float sinMag = 4f, sinScl = 6f, sinOffset = 60f, sideOffset = 0f, lenOffset = -1f;
    public TextureRegion region1, region2;

    @Override
    public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){

    }

    @Override
    public void drawBase(Building build){
        for(int i = 0; i < 4; i++){
            float len = Mathf.absin(build.totalProgress() + sinOffset + sideOffset * sinScl * i, sinScl, sinMag) + lenOffset;
            Draw.rect(i >= 2 ? region2 : region1, build.x + Geometry.d4[i].x * len, build.y + Geometry.d4[i].y * len, i * 90);
        }
    }

    @Override
    public void load(Block block){
        super.load(block);

        region1 = Core.atlas.find(block.name + "-piston0");
        region2 = Core.atlas.find(block.name + "-piston1");
    }
}
