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
    public void draw(GenericCrafterEntity entity){
        Draw.rect(entity.block.region, entity.x, entity.y);
        Draw.alpha(Mathf.absin(entity.totalProgress, glowScale, glowAmount) * entity.warmup);
        Draw.rect(top, entity.x, entity.y);
        Draw.reset();
    }

    @Override
    public void load(Block block){
        top = Core.atlas.find(block.name + "-top");
    }
}
