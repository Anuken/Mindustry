package mindustry.world.draw;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.Interp.*;
import arc.util.*;
import mindustry.gen.*;

public class DrawParticles extends DrawBlock{
    public Color color = Color.valueOf("f2d585");

    public float alpha = 0.5f;
    public int particles = 30;
    public float particleLife = 70f, particleRad = 7f, particleSize = 3f, fadeMargin = 0.4f, rotateScl = 3f;
    public boolean reverse = false;
    public Interp particleInterp = new PowIn(1.5f);
    public Interp particleSizeInterp = Interp.slope;
    public Blending blending = Blending.normal;

    @Override
    public void draw(Building build){

        if(build.warmup() > 0f){

            float a = alpha * build.warmup();
            Draw.blend(blending);
            Draw.color(color);

            float base = (Time.time / particleLife);
            rand.setSeed(build.id);
            for(int i = 0; i < particles; i++){
                float fin = (rand.random(2f) + base) % 1f;
                if(reverse) fin = 1f - fin;
                float fout = 1f - fin;
                float angle = rand.random(360f) + (Time.time / rotateScl) % 360f;
                float len = particleRad * particleInterp.apply(fout);
                Draw.alpha(a * (1f - Mathf.curve(fin, 1f - fadeMargin)));
                Fill.circle(
                    build.x + Angles.trnsx(angle, len),
                    build.y + Angles.trnsy(angle, len),
                    particleSize * particleSizeInterp.apply(fin) * build.warmup()
                );
            }

            Draw.blend();
            Draw.reset();
        }
    }
}
