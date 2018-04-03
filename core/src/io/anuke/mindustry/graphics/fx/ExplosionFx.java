package io.anuke.mindustry.graphics.fx;

import com.badlogic.gdx.graphics.Color;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Angles;

public class ExplosionFx {
    public static final Effect

    generatorexplosion = new Effect(28, 40f, e -> {
        Angles.randLenVectors(e.id, 16, 10f + e.ifract()*8f, (x, y)->{
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
    explosion = new Effect(11, e -> {
        Lines.stroke(2f*e.fract()+0.5f);
        Draw.color(Color.WHITE, Color.DARK_GRAY, e.powfract());
        Lines.circle(e.x, e.y, 5f + e.powfract() * 6f);

        Draw.color(e.ifract() < 0.5f ? Color.WHITE : Color.DARK_GRAY);
        Angles.randLenVectors(e.id, 5, 8f, (x, y)->{
            Fill.circle(e.x + x, e.y + y, e.fract()*5f + 2.5f);
        });

        Draw.reset();
    });
}
