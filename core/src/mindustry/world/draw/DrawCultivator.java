package mindustry.world.draw;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.production.GenericCrafter.*;

public class DrawCultivator extends DrawBlock{
    public Color plantColor = Color.valueOf("5541b1");
    public Color plantColorLight = Color.valueOf("7457ce");
    public Color bottomColor = Color.valueOf("474747");

    public int bubbles = 12, sides = 8;
    public float strokeMin = 0.2f, spread = 3f, timeScl = 70f;
    public float recurrence = 6f, radius = 3f;

    public TextureRegion middle;
    public TextureRegion top;

    @Override
    public void draw(GenericCrafterBuild build){
        Draw.rect(build.block.region, build.x, build.y);

        Drawf.liquid(middle, build.x, build.y, build.warmup, plantColor);

        Draw.color(bottomColor, plantColorLight, build.warmup);

        rand.setSeed(build.pos());
        for(int i = 0; i < bubbles; i++){
            float x = rand.range(spread), y = rand.range(spread);
            float life = 1f - ((Time.time / timeScl + rand.random(recurrence)) % recurrence);

            if(life > 0){
                Lines.stroke(build.warmup * (life + strokeMin));
                Lines.poly(build.x + x, build.y + y, sides, (1f - life) * radius);
            }
        }

        Draw.color();
        Draw.rect(top, build.x, build.y);
    }

    @Override
    public void load(Block block){
        middle = Core.atlas.find(block.name + "-middle");
        top = Core.atlas.find(block.name + "-top");
    }

    @Override
    public TextureRegion[] icons(Block block){
        return new TextureRegion[]{block.region, top};
    }
}
