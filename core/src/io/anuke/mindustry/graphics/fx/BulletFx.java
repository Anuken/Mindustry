package io.anuke.mindustry.graphics.fx;

import com.badlogic.gdx.graphics.Color;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.graphics.Shapes;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public class BulletFx {
    public static Color lightOrange = Color.valueOf("f68021");
    public static Color lighterOrange = Color.valueOf("f6e096");

    public static final Effect

    shootSmall = new Effect(8, e -> {
        Draw.color(lighterOrange, lightOrange, e.ifract());
        float w = 1f + 5 * e.fract();
        Shapes.tri(e.x, e.y, w, 15f * e.fract(), e.rotation);
        Shapes.tri(e.x, e.y, w, 3f * e.fract(), e.rotation + 180f);
        Draw.reset();
    }),

    smokeParticleSmall = new Effect(20, e -> {
        Draw.color(Color.GRAY);
        Fill.circle(e.x, e.y, e.fract()*1.5f);
        Draw.reset();
    }),

    hit = new Effect(14, e -> {
        Draw.color(Color.WHITE, lighterOrange, e.ifract());
        Lines.stroke(0.5f + e.fract());

        Angles.randLenVectors(e.id, 5, e.ifract()*15f, e.rotation, 50f, (x, y) -> {
            float ang = Mathf.atan2(x, y);
            Lines.lineAngle(e.x + x, e.y + y, ang, e.fract()*3 + 1f);
        });

        Draw.reset();
    });
}
