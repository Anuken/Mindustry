package mindustry.world.draw;

import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;

/** combined several DrawBlocks into one */
public class DrawMulti extends DrawBlock{
    public DrawBlock[] drawers = {};
    /** specifies the drawer index that sources the icon (since there can only be one icon source) */
    public int iconIndex = 0;

    public DrawMulti(){
    }

    public DrawMulti(DrawBlock... drawers){
        this.drawers = drawers;
    }

    public DrawMulti(Seq<DrawBlock> drawers){
        this.drawers = drawers.toArray(DrawBlock.class);
    }

    @Override
    public void drawBase(Building build){
        for(var draw : drawers){
            draw.drawBase(build);
        }
    }

    @Override
    public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){
        for(var draw : drawers){
            draw.drawPlan(block, plan, list);
        }
    }

    @Override
    public void drawLights(Building build){
        for(var draw : drawers){
            draw.drawLights(build);
        }
    }

    @Override
    public void load(Block block){
        for(var draw : drawers){
            draw.load(block);
        }
    }

    @Override
    public TextureRegion[] icons(Block block){
        return drawers.length <= iconIndex ? super.icons(block) : drawers[iconIndex].icons(block);
    }
}
