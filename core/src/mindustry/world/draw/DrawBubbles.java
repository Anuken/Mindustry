package mindustry.world.draw;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.gen.*;

public class DrawBubbles extends DrawBlock{
    public Color color = Color.valueOf("7457ce");

    public int amount = 12, sides = 8;
    public float strokeMin = 0.2f, spread = 3f, timeScl = 30f;
    public float recurrence = 6f, radius = 3f;
    public boolean fill = false;

    public DrawBubbles(Color color){
        this.color = color;
    }

    public DrawBubbles(){
    }

    @Override
    public void draw(Building build){
        if(build.warmup() <= 0.001f) return;

        Draw.color(color);
        Draw.alpha(build.warmup());

        rand.setSeed(build.id);
        for(int i = 0; i < amount; i++){
            float x = rand.range(spread), y = rand.range(spread);
            float life = 1f - ((Time.time / timeScl + rand.random(recurrence)) % recurrence);

            if(life > 0){
                float rad = (1f - life) * radius;
                if(fill){
                    Fill.circle(build.x + x, build.y + y, rad);
                }else{
                    Lines.stroke(build.warmup() * (life + strokeMin));
                    Lines.poly(build.x + x, build.y + y, sides, rad);
                }
            }
        }

        Draw.color();
    }
}
