package mindustry.world.draw;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.production.GenericCrafter.*;

/** Not standalone. */
public class DrawGlowRegion extends DrawBlock{
    public Blending blending = Blending.additive;
    public String suffix = "-glow";
    public float alpha = 0.9f, glowScale = 10f, glowIntensity = 0.5f;
    public float layer = Layer.blockAdditive;
    public Color color = Color.red.cpy();
    public TextureRegion top;

    @Override
    public void draw(GenericCrafterBuild build){
        if(build.warmup <= 0.001f) return;

        float z = Draw.z();
        Draw.z(layer);
        Draw.blend(blending);
        Draw.color(color);
        Draw.alpha((Mathf.absin(build.totalProgress, glowScale, alpha) * glowIntensity + 1f - glowIntensity) * build.warmup * alpha);
        Draw.rect(top, build.x, build.y);
        Draw.reset();
        Draw.blend();
        Draw.z(z);
    }

    @Override
    public void load(Block block){
        top = Core.atlas.find(block.name + suffix);
    }

    @Override
    public void drawPlan(GenericCrafter crafter, BuildPlan plan, Eachable<BuildPlan> list){}
}
