package io.anuke.mindustry.content.fx;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public class ExplosionFx {
    public static final Effect

    generatorexplosion = new Effect(28, 40f, e -> {
        Angles.randLenVectors(e.id, 16, 10f + e.ifract()*8f, (x, y) -> {
            float size = e.fract()*12f + 1f;
            Draw.color(Color.WHITE, Color.PURPLE, e.ifract());
            Draw.rect("circle", e.x + x, e.y + y, size, size);
            Draw.reset();
        });
    }),

    shockwave = new Effect(10f, 80f, e -> {
        Draw.color(Color.WHITE, Color.LIGHT_GRAY, e.ifract());
        Lines.stroke(e.fract()*2f + 0.2f);
        Lines.circle(e.x, e.y, e.ifract()*28f);
        Draw.reset();
    }),

    nuclearShockwave = new Effect(10f, 200f, e -> {
        Draw.color(Color.WHITE, Color.LIGHT_GRAY, e.ifract());
        Lines.stroke(e.fract()*3f + 0.2f);
        Lines.poly(e.x, e.y, 40, e.ifract()*140f);
        Draw.reset();
    }),

    explosion = new Effect(30, e -> {
        e.scaled(7, i -> {
            Lines.stroke(3f * i.fract());
            Lines.circle(e.x, e.y, 3f + i.ifract()*10f);
        });

        Draw.color(Color.GRAY);

        Angles.randLenVectors(e.id, 6, 2f + 19f * e.powfract(), (x, y) ->{
            Fill.circle(e.x + x, e.y + y, e.fract()*3f + 0.5f);
            Fill.circle(e.x + x/2f, e.y + y/2f, e.fract()*1f);
        });

        Draw.color(Palette.lighterOrange, Palette.lightOrange, Color.GRAY, e.ifract());
        Lines.stroke(1.5f * e.fract());

        Angles.randLenVectors(e.id + 1, 8, 1f + 23f * e.powfract(), (x, y) -> {
            Lines.lineAngle(e.x + x, e.y + y, Mathf.atan2(x, y), 1f + e.fract()*3f);
        });

        Draw.reset();
    });
}
