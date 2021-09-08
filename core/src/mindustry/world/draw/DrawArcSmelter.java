package mindustry.world.draw;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.world.*;
import mindustry.world.blocks.production.GenericCrafter.*;

public class DrawArcSmelter extends DrawBlock{
    public TextureRegion top, bottom;
    public Color flameColor = Color.valueOf("f58349"), midColor = Color.valueOf("f2d585");
    public float flameRad = 1f, circleSpace = 2f, flameRadiusScl = 3f, flameRadiusMag = 0.3f, circleStroke = 1.5f;

    public float alpha = 0.68f;
    public int particles = 25;
    public float particleLife = 40f, particleRad = 7f, particleStroke = 1.1f, particleLen = 3f;

    @Override
    public void draw(GenericCrafterBuild build){
        Draw.rect(bottom, build.x, build.y);

        if(build.warmup > 0f && flameColor.a > 0.001f){
            Lines.stroke(circleStroke * build.warmup);

            float si = Mathf.absin(flameRadiusScl, flameRadiusMag);
            float a = alpha * build.warmup;
            Draw.blend(Blending.additive);

            Draw.color(midColor, a);
            Fill.circle(build.x, build.y, flameRad + si);

            Draw.color(flameColor, a);
            Lines.circle(build.x, build.y, (flameRad + circleSpace + si) * build.warmup);

            Lines.stroke(particleStroke * build.warmup);

            float base = (Time.time / particleLife);
            rand.setSeed(build.id);
            for(int i = 0; i < particles; i++){
                float fin = (rand.random(1f) + base) % 1f, fout = 1f - fin;
                float angle = rand.random(360f);
                float len = particleRad * Interp.pow2Out.apply(fin);
                Lines.lineAngle(build.x + Angles.trnsx(angle, len), build.y + Angles.trnsy(angle, len), angle, particleLen * fout * build.warmup);
            }

            Draw.blend();
            Draw.reset();
        }

        Draw.rect(top, build.x, build.y);
        Draw.rect(build.block.region, build.x, build.y);
    }

    @Override
    public void load(Block block){
        top = Core.atlas.find(block.name + "-top");
        bottom = Core.atlas.find(block.name + "-bottom");
    }

    @Override
    public TextureRegion[] icons(Block block){
        return new TextureRegion[]{bottom, block.region, top};
    }
}
