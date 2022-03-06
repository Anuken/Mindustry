package mindustry.world.draw;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

public class DrawCells extends DrawBlock{
    public TextureRegion middle;
    public Color color = Color.white.cpy(), particleColorFrom = Color.black.cpy(), particleColorTo = Color.black.cpy();
    public int particles = 12;
    public float range = 4f, recurrence = 6f, radius = 3f, lifetime = 60f;

    @Override
    public void draw(Building build){
        Drawf.liquid(middle, build.x, build.y, build.warmup(), color);

        if(build.warmup() > 0.001f){
            rand.setSeed(build.id);
            for(int i = 0; i < particles; i++){
                float offset = rand.nextFloat() * 999999f;
                float x = rand.range(range), y = rand.range(range);
                float fin = 1f - (((Time.time + offset) / lifetime) % recurrence);
                float ca = rand.random(0.1f, 1f);
                float fslope = Mathf.slope(fin);

                if(fin > 0){
                    Draw.color(particleColorFrom, particleColorTo, ca);
                    Draw.alpha(build.warmup());

                    Fill.circle(build.x + x, build.y + y, fslope * radius);
                }
            }
        }

        Draw.color();
        Draw.rect(build.block.region, build.x, build.y);
    }

    @Override
    public void load(Block block){
        middle = Core.atlas.find(block.name + "-middle");
    }
}
