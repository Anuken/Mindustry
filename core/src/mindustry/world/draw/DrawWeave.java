package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.production.GenericCrafter.*;

public class DrawWeave extends DrawBlock{
    public TextureRegion weave, bottom;

    @Override
    public void draw(GenericCrafterBuild build){
        Draw.rect(bottom, build.x, build.y);
        Draw.rect(weave, build.x, build.y, build.totalProgress);

        Draw.color(Pal.accent);
        Draw.alpha(build.warmup);

        Lines.lineAngleCenter(
        build.x + Mathf.sin(build.totalProgress, 6f, Vars.tilesize / 3f * build.block.size),
        build.y,
        90,
        build.block.size * Vars.tilesize / 2f);

        Draw.reset();

        Draw.rect(build.block.region, build.x, build.y);
    }

    @Override
    public void load(Block block){
        weave = Core.atlas.find(block.name + "-weave");
        bottom = Core.atlas.find(block.name + "-bottom");
    }

    @Override
    public TextureRegion[] icons(Block block){
        return new TextureRegion[]{bottom, weave, block.region};
    }
}
