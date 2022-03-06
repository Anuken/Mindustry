package mindustry.world.draw;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.gen.*;

public class DrawCircles extends DrawBlock{
    public Color color = Color.valueOf("7457ce");

    public int amount = 5, sides = 15;
    public float strokeMin = 0.2f, strokeMax = 2f, timeScl = 160f;
    public float radius = 12f, radiusOffset = 0f, x = 0f, y = 0f;
    public Interp strokeInterp = Interp.pow3In;

    public DrawCircles(Color color){
        this.color = color;
    }

    public DrawCircles(){
    }

    @Override
    public void draw(Building build){
        if(build.warmup() <= 0.001f) return;

        Draw.color(color, build.warmup() * color.a);

        for(int i = 0; i < amount; i++){
            float life = ((Time.time / timeScl + i/(float)amount) % 1f);

            Lines.stroke(build.warmup() * strokeInterp.apply(strokeMax, strokeMin, life));
            Lines.poly(build.x + x, build.y + y, sides, radiusOffset + life * radius);
        }

        Draw.reset();
    }
}
