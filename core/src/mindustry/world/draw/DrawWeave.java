package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

public class DrawWeave extends DrawBlock{
    public TextureRegion weave;

    @Override
    public void draw(Building build){
        Draw.rect(weave, build.x, build.y, build.totalProgress());

        Draw.color(Pal.accent);
        Draw.alpha(build.warmup());

        Lines.lineAngleCenter(
        build.x + Mathf.sin(build.totalProgress(), 6f, Vars.tilesize / 3f * build.block.size),
        build.y,
        90,
        build.block.size * Vars.tilesize / 2f);

        Draw.reset();
    }

    @Override
    public TextureRegion[] icons(Block block){
        return new TextureRegion[]{weave};
    }

    @Override
    public void load(Block block){
        weave = Core.atlas.find(block.name + "-weave");
    }
}
