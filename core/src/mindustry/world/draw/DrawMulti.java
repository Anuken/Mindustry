package mindustry.world.draw;

import arc.graphics.g2d.*;
import mindustry.world.*;
import mindustry.world.blocks.production.GenericCrafter.*;

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

    @Override
    public void draw(GenericCrafterBuild build){
        for(var draw : drawers){
            draw.draw(build);
        }
    }

    @Override
    public void drawLight(GenericCrafterBuild build){
        for(var draw : drawers){
            draw.drawLight(build);
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
