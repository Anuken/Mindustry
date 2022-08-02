package mindustry.world.draw;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.Interp.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.world.*;

public class DrawSoftParticles extends DrawBlock{
    public TextureRegion region;

    public Color color = Color.valueOf("e3ae6f"), color2 = Color.valueOf("d04d46");

    public float alpha = 0.5f;
    public int particles = 30;
    public float particleLife = 70f, particleRad = 7f, particleSize = 3f, fadeMargin = 0.4f, rotateScl = 1.5f;
    public Interp particleInterp = new PowIn(1.5f);

    @Override
    public void draw(Building build){

        if(build.warmup() > 0f && color.a > 0.001f){
            float a = alpha * build.warmup();

            Draw.color(color, a);
            Draw.blend(Blending.additive);

            float base = (Time.time / particleLife);
            rand.setSeed(build.id);
            for(int i = 0; i < particles; i++){
                float fin = (rand.random(1f) + base) % 1f, fout = 1f - fin;
                fin = 1f - fin;
                fout = 1f - fout;

                float angle = rand.random(360f) + (Time.time / rotateScl) % 360f;
                float col = rand.random(1f);
                Draw.tint(color, color2, col);
                float len = particleRad * particleInterp.apply(fout);
                Draw.alpha(a * (1f - Mathf.curve(fin, 1f - fadeMargin)));
                float r = particleSize * fin * build.warmup()*2f;
                Draw.rect(
                    region,
                    build.x + Angles.trnsx(angle, len),
                    build.y + Angles.trnsy(angle, len),
                    r, r
                );
            }

            Draw.blend();
            Draw.reset();
        }
    }

    @Override
    public void load(Block block){
        super.load(block);

        region = Core.atlas.find("circle-shadow");
    }
}
