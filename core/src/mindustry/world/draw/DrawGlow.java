package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.world.*;
import mindustry.world.blocks.production.GenericCrafter.*;

public class DrawGlow extends DrawBlock{
    public float glowAmount = 0.9f, glowScale = 3f;
    public TextureRegion top;

    @Override
    public void draw(GenericCrafterBuild build){
        Draw.rect(build.block.region, build.x, build.y);
        Draw.alpha(Mathf.absin(build.totalProgress, glowScale, glowAmount) * build.warmup);
        Draw.rect(top, build.x, build.y);
        Draw.reset();
    }

    @Override
    public void load(Block block){
        top = Core.atlas.find(block.name + "-top");
    }
}
