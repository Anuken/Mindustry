package mindustry.world.draw.projections;

import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.Projector.*;

import static mindustry.Vars.*;

public class DrawMendProjection{
    public void draw(ProjectorBuild build){
        float f = 1f - (Time.time / 100f) % 1f;

        size = build.block.size;

        Draw.color(build.block.baseColor, build.block.phaseColor, build.phaseHeat);
        Draw.alpha(build.heat * Mathf.absin(Time.time, 10f, 1f) * 0.5f);
        Draw.rect(build.block.topRegion, build.x, build.y);
        Draw.alpha(1f);
        Lines.stroke((2f * f + 0.2f) * build.heat);
        Lines.square(build.x, build.y, Math.min(1f + (1f - f) * size * tilesize / 2f, size * tilesize/2f));

        Draw.reset();
    }
}
