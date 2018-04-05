package io.anuke.mindustry.content.fx;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.entities.effect.StaticEffectEntity.StaticEffect;
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

    shootBig2 = new Effect(10, e -> {
        Draw.color(Palette.lightOrange, Color.GRAY, e.ifract());
        float w = 1.2f + 8 * e.fract();
        Shapes.tri(e.x, e.y, w, 29f * e.fract(), e.rotation);
        Shapes.tri(e.x, e.y, w, 5f * e.fract(), e.rotation + 180f);
        Draw.reset();
    }),


    shootBigSmoke = new Effect(17f, e -> {
        Draw.color(Palette.lighterOrange, Color.LIGHT_GRAY, Color.GRAY, e.ifract());

        Angles.randLenVectors(e.id, 8, e.powfract()*19f, e.rotation, 10f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fract()*2f + 0.2f);
        });

        Draw.reset();
    }),

    shootBigSmoke2 = new Effect(18f, e -> {
        Draw.color(Palette.lightOrange, Color.LIGHT_GRAY, Color.GRAY, e.ifract());

        Angles.randLenVectors(e.id, 9, e.powfract()*23f, e.rotation, 20f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fract()*2.4f + 0.2f);
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

    shellEjectSmall = new StaticEffect(30f, 400f, e -> {
        Draw.color(Palette.lightOrange, Color.LIGHT_GRAY, Palette.lightishGray, e.ifract());
        float rot = e.rotation + 90f;
        for(int i : Mathf.signs){
            float len = (2f + e.powfract()*6f) * i;
            float lr = rot + e.ifract()*30f*i;
            Draw.rect("white",
                    e.x + Angles.trnsx(lr, len) + Mathf.randomSeedRange(e.id + i + 7, 3f * e.ifract()),
                    e.y + Angles.trnsy(lr, len) + Mathf.randomSeedRange(e.id + i + 8, 3f * e.ifract()),
                    1f, 2f, rot + e.ifract()*50f*i);
        }

        Draw.color();
    }),

    shellEjectMedium = new StaticEffect(34f, 400f, e -> {
        Draw.color(Palette.lightOrange, Color.LIGHT_GRAY, Palette.lightishGray, e.ifract());
        float rot = e.rotation + 90f;
        for(int i : Mathf.signs){
            float len = (2f + e.powfract()*10f) * i;
            float lr = rot + e.ifract()*20f*i;
            Draw.rect("casing",
                    e.x + Angles.trnsx(lr, len) + Mathf.randomSeedRange(e.id + i + 7, 3f * e.ifract()),
                    e.y + Angles.trnsy(lr, len) + Mathf.randomSeedRange(e.id + i + 8, 3f * e.ifract()),
                    2f, 3f, rot);
        }

        Draw.color(Color.LIGHT_GRAY, Color.GRAY, e.ifract());

        for(int i : Mathf.signs){
            Angles.randLenVectors(e.id, 4, 1f + e.powfract()*11f, e.rotation + 90f*i, 20f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fract()*1.5f);
            });
        }

        Draw.color();
    }),

    shellEjectBig = new StaticEffect(22f, 400f, e -> {
        Draw.color(Palette.lightOrange, Color.LIGHT_GRAY, Palette.lightishGray, e.ifract());
        float rot = e.rotation + 90f;
        for(int i : Mathf.signs){
            float len = (4f + e.powfract()*8f) * i;
            float lr = rot  + Mathf.randomSeedRange(e.id + i + 6, 20f * e.ifract())*i;
            Draw.rect("casing",
                    e.x + Angles.trnsx(lr, len) + Mathf.randomSeedRange(e.id + i + 7, 3f * e.ifract()),
                    e.y + Angles.trnsy(lr, len) + Mathf.randomSeedRange(e.id + i + 8, 3f * e.ifract()),
                    2.5f, 4f,
                    rot+ e.ifract()*30f*i + Mathf.randomSeedRange(e.id + i + 9, 40f * e.ifract()));
        }

        Draw.color(Color.LIGHT_GRAY);

        for(int i : Mathf.signs){
            Angles.randLenVectors(e.id, 4, -e.powfract()*15f, e.rotation + 90f*i, 25f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fract()*2f);
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

    hitLancer = new Effect(12, e -> {
        Draw.color(Color.WHITE);
        Lines.stroke(e.fract()*1.5f);

        Angles.randLenVectors(e.id, 8, e.powfract()*17f, e.rotation, 360f, (x, y) -> {
            float ang = Mathf.atan2(x, y);
            Lines.lineAngle(e.x + x, e.y + y, ang, e.fract()*4 + 1f);
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
    }),

    flakExplosion = new Effect(20, e -> {

        Draw.color(Palette.bulletYellow);
        e.scaled(6, i -> {
            Lines.stroke(3f * i.fract());
            Lines.circle(e.x, e.y, 3f + i.ifract()*10f);
        });

        Draw.color(Color.GRAY);

        Angles.randLenVectors(e.id, 5, 2f + 23f * e.powfract(), (x, y) ->{
            Fill.circle(e.x + x, e.y + y, e.fract()*3f + 0.5f);
        });

        Draw.color(Palette.lighterOrange);
        Lines.stroke(1f * e.fract());

        Angles.randLenVectors(e.id + 1, 4, 1f + 23f * e.powfract(), (x, y) -> {
            Lines.lineAngle(e.x + x, e.y + y, Mathf.atan2(x, y), 1f + e.fract()*3f);
        });

        Draw.reset();
    }),

    lancerLaserShoot = new Effect(12f, e -> {
        Draw.color(Palette.lancerLaser);

        for(int i : Mathf.signs){
            Shapes.tri(e.x, e.y, 4f * e.fract(), 29f, e.rotation + 90f*i);
        }

        Draw.reset();
    }),

    lancerLaserShootSmoke = new Effect(20f, e -> {
        Draw.color(Palette.lancerLaser);

        Angles.randLenVectors(e.id, 7, 80f, e.rotation, 0f, (x, y) -> {
            Lines.lineAngle(e.x + x, e.y + y, Mathf.atan2(x, y), e.fract()*9f);
        });

        Draw.reset();
    });
}
