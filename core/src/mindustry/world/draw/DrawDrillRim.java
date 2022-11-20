package mindustry.world.draw;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.world.Block;
import mindustry.world.blocks.production.*;

public class DrawDrillRim extends DrawBlock{
    float s = 0.3f;
    float ts = 0.6f;
    Color color = Color.valueOf("ff5512");
    TextureRegion rim;

    public DrawDrillRim(){}

    public DrawDrillRim(Color color){
        this.color = color;
    }

    @Override
    public void draw(Building build) {
        if(!(build instanceof Drill.DrillBuild drill) || drill.dominantItem == null) return;

        Draw.color(color);
        Draw.alpha(drill.warmup * ts * (1f - s + Mathf.absin(Time.time, 3f, s)));
        Draw.blend(Blending.additive);
        Draw.rect(rim, build.x, build.y);
        Draw.blend();
        Draw.color();
    }

    @Override
    public void load(Block block){
        rim = Core.atlas.find(block.name + "-rim");
    }
}
