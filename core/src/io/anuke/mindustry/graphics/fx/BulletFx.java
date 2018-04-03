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

    shootSmallSmoke = new Effect(20f, e -> {
        Draw.color(Color.GRAY);

        Angles.randLenVectors(e.id, 5, e.powfract()*6f, e.rotation, 20f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fract()*1.5f);
        });

        Draw.reset();
    }),

    shellEjectSmall = new Effect(30f, e -> {
        Draw.color(Color.LIGHT_GRAY);
        Draw.alpha(e.fract());
        float rot = e.rotation + 90f;
        for(int i : Mathf.signs){
            float len = (2f + e.powfract()*6f) * i;
            float lr = rot + e.ifract()*30f*i;
            Draw.rect("white", e.x + Angles.trnsx(lr, len), e.y + Angles.trnsy(lr, len), 1f, 2f, rot + e.ifract()*50f*i);
        }
        Draw.color();
    }),

    hit = new Effect(14, e -> {
        Draw.color(Color.WHITE, lightOrange, e.ifract());
        Lines.stroke(0.5f + e.fract());

        Angles.randLenVectors(e.id, 5, e.ifract()*15f, e.rotation, 50f, (x, y) -> {
            float ang = Mathf.atan2(x, y);
            Lines.lineAngle(e.x + x, e.y + y, ang, e.fract()*3 + 1f);
        });

        Draw.reset();
    });
}
