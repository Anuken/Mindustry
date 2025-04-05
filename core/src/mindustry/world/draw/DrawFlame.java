package mindustry.world.draw;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

public class DrawFlame extends DrawBlock{
    public Color flameColor = Color.valueOf("ffc999");
    public TextureRegion top;
    public float lightRadius = 60f, lightAlpha = 0.65f, lightSinScl = 10f, lightSinMag = 5;
    public float flameRadius = 3f, flameRadiusIn = 1.9f, flameRadiusScl = 5f, flameRadiusMag = 2f, flameRadiusInMag = 1f;
    public float flameX = 0, flameY = 0;

    public DrawFlame(){
    }

    public DrawFlame(Color flameColor){
        this.flameColor = flameColor;
    }

    @Override
    public void load(Block block){
        block.emitLight = true;
        top = Core.atlas.find(block.name + "-top");
        block.lightClipSize = Math.max(block.lightClipSize, (lightRadius + lightSinMag) * 2f * block.size);
    }

    @Override
    public void draw(Building build){
        if(build.warmup() > 0f && flameColor.a > 0.001f){
            float g = 0.3f;
            float r = 0.06f;
            float cr = Mathf.random(0.1f);

            Draw.z(Layer.block + 0.01f);

            Draw.alpha(build.warmup());
            Draw.rect(top, build.x, build.y);

            Draw.alpha(((1f - g) + Mathf.absin(Time.time, 8f, g) + Mathf.random(r) - r) * build.warmup());

            Draw.tint(flameColor);
            Fill.circle(build.x + flameX, build.y + flameY, flameRadius + Mathf.absin(Time.time, flameRadiusScl, flameRadiusMag) + cr);
            Draw.color(1f, 1f, 1f, build.warmup());
            Fill.circle(build.x + flameX, build.y + flameY, flameRadiusIn + Mathf.absin(Time.time, flameRadiusScl, flameRadiusInMag) + cr);

            Draw.color();
        }
    }

    @Override
    public void drawLight(Building build){
        Drawf.light(build.x + flameX, build.y + flameY, (lightRadius + Mathf.absin(lightSinScl, lightSinMag)) * build.warmup() * build.block.size, flameColor, lightAlpha);
    }
}
