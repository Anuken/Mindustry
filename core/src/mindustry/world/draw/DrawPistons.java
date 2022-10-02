package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;

public class DrawPistons extends DrawBlock{
    public float sinMag = 4f, sinScl = 6f, sinOffset = 50f, sideOffset = 0f, lenOffset = -1f;
    public int sides = 4;
    public TextureRegion region1, region2, regiont;

    @Override
    public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){

    }

    @Override
    public void draw(Building build){
        for(int i = 0; i < sides; i++){
            float len = Mathf.absin(build.totalProgress() + sinOffset + sideOffset * sinScl * i, sinScl, sinMag) + lenOffset;
            float angle = i * 360f / sides;
            TextureRegion reg =
                regiont.found() && (Mathf.equal(angle, 315) || Mathf.equal(angle, 135)) ? regiont :
                angle >= 135 && angle < 315 ? region2 : region1;

            if(Mathf.equal(angle, 315)){
                Draw.yscl = -1f;
            }

            Draw.rect(reg, build.x + Angles.trnsx(angle, len), build.y + Angles.trnsy(angle, len), angle);

            Draw.yscl = 1f;
        }
    }

    @Override
    public void load(Block block){
        super.load(block);

        region1 = Core.atlas.find(block.name + "-piston0", block.name + "-piston");
        region2 = Core.atlas.find(block.name + "-piston1", block.name + "-piston");
        regiont = Core.atlas.find(block.name + "-piston-t");
    }
}
