package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;

/** An implementation of custom rendering behavior for a crafter block.
 * This is used mostly for mods. */
public abstract class DrawBlock{
    protected static final Rand rand = new Rand();

    /** If set, the icon is overridden to be these strings, in order. Each string is a suffix. */
    public @Nullable String[] iconOverride = null;

    public void getRegionsToOutline(Block block, Seq<TextureRegion> out){

    }

    /** Draws the block itself. */
    public void draw(Building build){

    }

    /** Draws any extra light for the block. */
    public void drawLight(Building build){

    }

    /** Draws the planned version of this block. */
    public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){

    }

    /** Load any relevant texture regions. */
    public void load(Block block){

    }

    /** @return the generated icons to be used for this block. */
    public TextureRegion[] icons(Block block){
        return new TextureRegion[]{block.region};
    }

    public final TextureRegion[] finalIcons(Block block){
        if(iconOverride != null){
            var out = new TextureRegion[iconOverride.length];
            for(int i = 0; i < out.length; i++){
                out[i] = Core.atlas.find(block.name + iconOverride[i]);
            }
            return out;
        }
        return icons(block);
    }

    public GenericCrafter expectCrafter(Block block){
        if(!(block instanceof GenericCrafter crafter)) throw new ClassCastException("This drawer requires the block to be a GenericCrafter. Use a different drawer.");
        return crafter;
    }
}
