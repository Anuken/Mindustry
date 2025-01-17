package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

public class DrawWeaveSimple extends DrawBlock {
    public TextureRegion weave;

    @Override
    public void draw(Building build) {
        Draw.rect(weave, build.x, build.y, build.totalProgress());

        Draw.color(Pal.accent);
        Draw.alpha(build.warmup());

        Draw.reset();
    }

    @Override
    public TextureRegion[] icons(Block block) {
        return new TextureRegion[]{weave};
    }

    @Override
    public void load(Block block) {
        weave = Core.atlas.find(block.name + "-weave");
    }
}