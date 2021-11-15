package mindustry.world.draw;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.production.GenericCrafter.*;

/** An implementation of custom rendering behavior for a crafter block.
 * This is used mostly for mods. */
public class DrawBlock{
    protected static final Rand rand = new Rand();

    /** Draws the block. */
    public void draw(GenericCrafterBuild build){
        Draw.rect(build.block.region, build.x, build.y, build.block.rotate ? build.rotdeg() : 0);
    }

    /** Draws any extra light for the block. */
    public void drawLight(GenericCrafterBuild build){

    }

    /** Draws the planned version of this block. */
    public void drawPlan(GenericCrafter crafter, BuildPlan plan, Eachable<BuildPlan> list){
        crafter.drawPlanBase(plan, list);
    }

    /** Load any relevant texture regions. */
    public void load(Block block){

    }

    /** @return the generated icons to be used for this block. */
    public TextureRegion[] icons(Block block){
        return new TextureRegion[]{block.region};
    }
}
