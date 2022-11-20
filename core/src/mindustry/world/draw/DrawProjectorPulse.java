package mindustry.world.draw;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.Block;

import static mindustry.Vars.*;

public class DrawProjectorPulse extends DrawBlock{
    public Color color = Pal.accent.cpy();
    public float stroke = 2f, timeScl = 100f, minStroke = 0.2f;
    public float radiusScl = 1f;
    public float layer = -1f;
    public boolean square = true;

    public boolean drawLines = true;
    public boolean drawRegion = true;
    public String suffix = "-top";
    public TextureRegion pulse;

    public DrawProjectorPulse(boolean square, Color color, boolean drawRegion){
        this.square = square;
        this.color = color;
        this.drawRegion = drawRegion;
    }

    public DrawProjectorPulse(boolean square, Color color){
        this.square = square;
        this.color = color;
    }

    public DrawProjectorPulse(boolean square){
        this.square = square;
    }

    public DrawProjectorPulse(){}

    @Override
    public void draw(Building build){
        float pz = Draw.z();
        if(layer > 0) Draw.z(layer);

        float f = 1f - (Time.time / timeScl) % 1f;
        float rad = build.block.size * tilesize / 2f * radiusScl;

        Draw.color(color);

        if(pulse != null || pulse.found()){
            Draw.alpha(build.warmup() * Mathf.absin(Time.time, 50f / Mathf.PI2, 1f) * 0.5f);
            Draw.rect(pulse, build.x, build.y);
            Draw.alpha(1f);
        }

        if(drawLines) {
            Lines.stroke((stroke * f + minStroke) * build.warmup());
            if (square) {
                Lines.square(build.x, build.y, Math.min(1f + (1f - f) * rad, rad));
            } else {
                float r = Math.max(0f, Mathf.clamp(2f - f * 2f) * rad - f - 0.2f), w = Mathf.clamp(0.5f - f) * rad * 2f;
                Lines.beginLine();
                for (int i = 0; i < 4; i++) {
                    Lines.linePoint(build.x + Geometry.d4(i).x * r + Geometry.d4(i).y * w, build.y + Geometry.d4(i).y * r - Geometry.d4(i).x * w);
                    if (f < 0.5f)
                        Lines.linePoint(build.x + Geometry.d4(i).x * r - Geometry.d4(i).y * w, build.y + Geometry.d4(i).y * r + Geometry.d4(i).x * w);
                }
                Lines.endLine(true);
            }
        }

        Draw.reset();
        Draw.z(pz);
    }

    @Override
    public void load(Block block){
        pulse = Core.atlas.find(block.name + suffix);
    }
}
