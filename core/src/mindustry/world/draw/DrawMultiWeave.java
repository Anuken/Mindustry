package mindustry.world.draw;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.gen.*;
import mindustry.world.*;

public class DrawMultiWeave extends DrawBlock{
    public TextureRegion weave, glow;
    public float rotateSpeed = 1f, rotateSpeed2 = -0.9f;
    public boolean fadeWeave = false;
    public Color glowColor = new Color(1f, 0.4f, 0.4f, 0.8f), weaveColor = Color.white.cpy();
    public float pulse = 0.3f, pulseScl = 10f;

    @Override
    public void draw(Building build){
        Draw.color(weaveColor);
        if(fadeWeave){
            Draw.alpha(build.warmup());
        }

        Draw.rect(weave, build.x, build.y, build.totalProgress() * rotateSpeed);
        Draw.rect(weave, build.x, build.y, build.totalProgress() * rotateSpeed * rotateSpeed2);

        Draw.blend(Blending.additive);

        Draw.color(glowColor, build.warmup() * (glowColor.a * (1f - pulse + Mathf.absin(pulseScl, pulse))));

        Draw.rect(glow, build.x, build.y, build.totalProgress() * rotateSpeed);
        Draw.rect(glow, build.x, build.y, build.totalProgress() * rotateSpeed * rotateSpeed2);

        Draw.blend();
        Draw.reset();
    }

    @Override
    public TextureRegion[] icons(Block block){
        return fadeWeave ? new TextureRegion[0] : new TextureRegion[]{weave};
    }

    @Override
    public void load(Block block){
        weave = Core.atlas.find(block.name + "-weave");
        glow = Core.atlas.find(block.name + "-weave-glow");
    }

}
