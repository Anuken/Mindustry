package mindustry.world.draw.projections;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.Projector.*;

import static mindustry.Vars.*;

public class DrawOverdriveProjection{
    public void draw(ProjectorBuild build){
        float f = 1f - (Time.time / 100f) % 1f;

        size = build.block.size;

        Draw.color(build.block.baseColor, build.block.phaseColor, build.phaseHeat);
        Draw.alpha(build.heat * Mathf.absin(Time.time, 10f, 1f) * 0.5f);
        Draw.rect(build.block.topRegion, build.x, build.y);
        Draw.alpha(1f);
        Lines.stroke((2f * f + 0.1f) * build.heat);

        float r = Math.max(0f, Mathf.clamp(2f - f * 2f) * size * tilesize / 2f - f - 0.2f), w = Mathf.clamp(0.5f - f) * size * tilesize;
        Lines.beginLine();
        for(int i = 0; i < 4; i++){
            Lines.linePoint(build.x + Geometry.d4(i).x * r + Geometry.d4(i).y * w, build.y + Geometry.d4(i).y * r - Geometry.d4(i).x * w);
            if(f < 0.5f) Lines.linePoint(x + Geometry.d4(i).x * r - Geometry.d4(i).y * w, y + Geometry.d4(i).y * r + Geometry.d4(i).x * w);
        }
        Lines.endLine(true);

        Draw.reset();
    }
}
