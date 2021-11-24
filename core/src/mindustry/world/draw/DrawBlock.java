package mindustry.world.draw;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.production.GenericCrafter.*;

/** An implementation of custom rendering behavior for a crafter block.
 * This is used mostly for mods. */
public class DrawBlock{
    protected static final Rand rand = new Rand();

    /** @deprecated no longer called! not specific to generic crafters! */
    @Deprecated
    public void draw(GenericCrafterBuild build){}

    /** @deprecated no longer called! not specific to generic crafters! */
    @Deprecated
    public void drawLight(GenericCrafterBuild build){}

    public void getRegionsToOutline(Seq<TextureRegion> out){

    }

    /** Draws the block itself. */
    public void drawBase(Building build){
        Draw.rect(build.block.region, build.x, build.y, build.drawrot());
    }

    /** Draws any extra light for the block. */
    public void drawLights(Building build){

    }

    /** Draws the planned version of this block. */
    public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){
        block.drawDefaultRequestRegion(plan, list);
    }

    /** Load any relevant texture regions. */
    public void load(Block block){

    }

    /** @return the generated icons to be used for this block. */
    public TextureRegion[] icons(Block block){
        return new TextureRegion[]{block.region};
    }

    public GenericCrafter expectCrafter(Block block){
        if(!(block instanceof GenericCrafter crafter)) throw new ClassCastException("This drawer requires the block to be a GenericCrafter. Use a different drawer.");
        return crafter;
    }
}
