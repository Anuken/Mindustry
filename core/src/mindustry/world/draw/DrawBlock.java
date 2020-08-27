package mindustry.world.draw;

import arc.graphics.g2d.*;
import mindustry.world.*;
import mindustry.world.blocks.production.GenericCrafter.*;

/** An implementation of custom rendering behavior for a block.
 * This is used mostly for mods. */
public class DrawBlock{

    /** Draws the block. */
    public void draw(GenericCrafterBuild entity){
        Draw.rect(entity.block.region, entity.x, entity.y, entity.block.rotate ? entity.rotdeg() : 0);
    }

    /** Load any relevant texture regions. */
    public void load(Block block){

    }

    /** @return the generated icons to be used for this block. */
    public TextureRegion[] icons(Block block){
        return new TextureRegion[]{block.region};
    }
}
