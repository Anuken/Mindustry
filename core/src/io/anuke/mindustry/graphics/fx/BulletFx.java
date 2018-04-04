package io.anuke.mindustry.graphics.fx;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.graphics.Shapes;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public class BulletFx {

    public static final Effect

    shootSmall = new Effect(8, e -> {
        Draw.color(Palette.lighterOrange, Palette.lightOrange, e.ifract());
        float w = 1f + 5 * e.fract();
        Shapes.tri(e.x, e.y, w, 15f * e.fract(), e.rotation);
        Shapes.tri(e.x, e.y, w, 3f * e.fract(), e.rotation + 180f);
        Draw.reset();
    }),

    shootSmallSmoke = new Effect(20f, e -> {
        Draw.color(Palette.lighterOrange, Color.LIGHT_GRAY, Color.GRAY, e.ifract());

        Angles.randLenVectors(e.id, 5, e.powfract()*6f, e.rotation, 20f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fract()*1.5f);
        });

        Draw.reset();
    }),

    shootBig = new Effect(9, e -> {
        Draw.color(Palette.lighterOrange, Palette.lightOrange, e.ifract());
        float w = 1.2f + 7 * e.fract();
        Shapes.tri(e.x, e.y, w, 25f * e.fract(), e.rotation);
        Shapes.tri(e.x, e.y, w, 4f * e.fract(), e.rotation + 180f);
        Draw.reset();
    }),

    shootBigSmoke = new Effect(17f, e -> {
        Draw.color(Palette.lighterOrange, Color.LIGHT_GRAY, Color.GRAY, e.ifract());

        Angles.randLenVectors(e.id, 8, e.powfract()*19f, e.rotation, 10f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fract()*2f + 0.2f);
        });

        Draw.reset();
    }),

    shootSmallFlame = new Effect(30f, e -> {
        Draw.color(Palette.lightFlame, Palette.darkFlame, Color.GRAY, e.ifract());

        Angles.randLenVectors(e.id, 8, e.powfract()*26f, e.rotation, 10f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.65f + e.fract()*1.5f);
        });

        Draw.reset();
    }),

    shellEjectSmall = new Effect(30f, e -> {
        Draw.color(Palette.lightOrange, Color.LIGHT_GRAY, Color.GRAY, e.ifract());
        Draw.alpha(e.fract());
        float rot = e.rotation + 90f;
        for(int i : Mathf.signs){
            float len = (2f + e.powfract()*6f) * i;
            float lr = rot + e.ifract()*30f*i;
            Draw.rect("white", e.x + Angles.trnsx(lr, len), e.y + Angles.trnsy(lr, len), 1f, 2f, rot + e.ifract()*50f*i);
        }

        Draw.color();
    }),

    shellEjectBig = new Effect(30f, e -> {
        Draw.color(Palette.lightOrange, Color.LIGHT_GRAY, Color.GRAY, e.ifract());
        float rot = e.rotation + 90f;
        for(int i : Mathf.signs){
            float len = (2f + e.powfract()*10f) * i;
            float lr = rot + e.ifract()*20f*i;
            Draw.rect("white", e.x + Angles.trnsx(lr, len), e.y + Angles.trnsy(lr, len), 2f, 3f, rot);
        }

        Draw.color(Color.LIGHT_GRAY, Color.GRAY, e.ifract());

        for(int i : Mathf.signs){
            Angles.randLenVectors(e.id, 4, 1f + e.powfract()*11f, e.rotation + 90f*i, 20f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fract()*1.5f);
            });
        }

        Draw.color();
    }),

    hitBulletSmall = new Effect(14, e -> {
        Draw.color(Color.WHITE, Palette.lightOrange, e.ifract());
        Lines.stroke(0.5f + e.fract());

        Angles.randLenVectors(e.id, 5, e.ifract()*15f, e.rotation, 50f, (x, y) -> {
            float ang = Mathf.atan2(x, y);
            Lines.lineAngle(e.x + x, e.y + y, ang, e.fract()*3 + 1f);
        });

        Draw.reset();
    }),

    hitBulletBig = new Effect(13, e -> {
        Draw.color(Color.WHITE, Palette.lightOrange, e.ifract());
        Lines.stroke(0.5f + e.fract()*1.5f);

        Angles.randLenVectors(e.id, 8, e.powfract()*30f, e.rotation, 50f, (x, y) -> {
            float ang = Mathf.atan2(x, y);
            Lines.lineAngle(e.x + x, e.y + y, ang, e.fract()*4 + 1.5f);
        });

        Draw.reset();
    }),

    hitFlameSmall = new Effect(14, e -> {
        Draw.color(Palette.lightFlame, Palette.darkFlame, e.ifract());
        Lines.stroke(0.5f + e.fract());

        Angles.randLenVectors(e.id, 5, e.ifract()*15f, e.rotation, 50f, (x, y) -> {
            float ang = Mathf.atan2(x, y);
            Lines.lineAngle(e.x + x, e.y + y, ang, e.fract()*3 + 1f);
        });

        Draw.reset();
    }),

    despawn = new Effect(12, e -> {
        Draw.color(Palette.lighterOrange, Color.GRAY, e.ifract());
        Lines.stroke(e.fract());

        Angles.randLenVectors(e.id, 7, e.ifract()*7f, e.rotation, 40f, (x, y) -> {
            float ang = Mathf.atan2(x, y);
            Lines.lineAngle(e.x + x, e.y + y, ang, e.fract()*2 + 1f);
        });

        Draw.reset();
    });
}
