package mindustry.world.draw;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

/** Not standalone. */
public class DrawGlowRegion extends DrawBlock{
    public Blending blending = Blending.additive;
    public String suffix = "-glow";
    public float alpha = 0.9f, glowScale = 10f, glowIntensity = 0.5f;
    public float rotateSpeed = 0f;
    public float layer = Layer.blockAdditive;
    public boolean rotate = false;
    public Color color = Color.red.cpy();
    public TextureRegion region;

    public DrawGlowRegion(){
    }

    public DrawGlowRegion(float layer){
        this.layer = layer;
    }

    public DrawGlowRegion(boolean rotate){
        this.rotate = rotate;
    }


    public DrawGlowRegion(String suffix){
        this.suffix = suffix;
    }

    @Override
    public void draw(Building build){
        if(build.warmup() <= 0.001f) return;

        float z = Draw.z();
        if(layer > 0) Draw.z(layer);
        Draw.blend(blending);
        Draw.color(color);
        Draw.alpha((Mathf.absin(build.totalProgress(), glowScale, alpha) * glowIntensity + 1f - glowIntensity) * build.warmup() * alpha);
        Draw.rect(region, build.x, build.y, build.totalProgress() * rotateSpeed + (rotate ? build.rotdeg() : 0f));
        Draw.reset();
        Draw.blend();
        Draw.z(z);
    }

    @Override
    public void load(Block block){
        region = Core.atlas.find(block.name + suffix);
    }
}
