package mindustry.world.draw;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

public class DrawCultivator extends DrawBlock{
    public Color plantColor = Color.valueOf("5541b1");
    public Color plantColorLight = Color.valueOf("7457ce");
    public Color bottomColor = Color.valueOf("474747");

    public int bubbles = 12, sides = 8;
    public float strokeMin = 0.2f, spread = 3f, timeScl = 70f;
    public float recurrence = 6f, radius = 3f;

    public TextureRegion middle;

    @Override
    public void draw(Building build){
        Drawf.liquid(middle, build.x, build.y, build.warmup(), plantColor);

        Draw.color(bottomColor, plantColorLight, build.warmup());

        rand.setSeed(build.pos());
        for(int i = 0; i < bubbles; i++){
            float x = rand.range(spread), y = rand.range(spread);
            float life = 1f - ((Time.time / timeScl + rand.random(recurrence)) % recurrence);

            if(life > 0){
                Lines.stroke(build.warmup() * (life + strokeMin));
                Lines.poly(build.x + x, build.y + y, sides, (1f - life) * radius);
            }
        }

        Draw.color();
    }

    @Override
    public void load(Block block){
        middle = Core.atlas.find(block.name + "-middle");
    }
}
