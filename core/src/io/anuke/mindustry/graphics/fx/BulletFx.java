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
    public static final Effect

    chainshot = new Effect(9f, e -> {
        Draw.color(Color.WHITE, Fx.lightOrange, e.ifract());
        Lines.stroke(e.fract()*4f);
        Lines.lineAngle(e.x, e.y, e.rotation, e.fract()*7f);
        Lines.stroke(e.fract()*2f);
        Lines.lineAngle(e.x, e.y, e.rotation, e.fract()*10f);
        Draw.reset();
    }),
    mortarshot = new Effect(10f, e -> {
        Draw.color(Color.WHITE, Color.DARK_GRAY, e.ifract());
        Lines.stroke(e.fract()*6f);
        Lines.lineAngle(e.x, e.y, e.rotation, e.fract()*10f);
        Lines.stroke(e.fract()*5f);
        Lines.lineAngle(e.x, e.y, e.rotation, e.fract()*14f);
        Lines.stroke(e.fract()*1f);
        Lines.lineAngle(e.x, e.y, e.rotation, e.fract()*16f);
        Draw.reset();
    }),
    railshot = new Effect(9f, e -> {
        Draw.color(Color.WHITE, Color.DARK_GRAY, e.ifract());
        Lines.stroke(e.fract()*5f);
        Lines.lineAngle(e.x, e.y, e.rotation, e.fract()*8f);
        Lines.stroke(e.fract()*4f);
        Lines.lineAngle(e.x, e.y, e.rotation, e.fract()*12f);
        Lines.stroke(e.fract()*1f);
        Lines.lineAngle(e.x, e.y, e.rotation, e.fract()*14f);
        Draw.reset();
    }),
    titanshot = new Effect(12f, e -> {
        Draw.color(Color.WHITE, Fx.lightOrange, e.ifract());
        Lines.stroke(e.fract()*7f);
        Lines.lineAngle(e.x, e.y, e.rotation, e.fract()*12f);
        Lines.stroke(e.fract()*4f);
        Lines.lineAngle(e.x, e.y, e.rotation, e.fract()*16f);
        Lines.stroke(e.fract()*2f);
        Lines.lineAngle(e.x, e.y, e.rotation, e.fract()*18f);
        Draw.reset();
    }),
    shockwaveSmall = new Effect(10f, e -> {
        Draw.color(Color.WHITE, Color.LIGHT_GRAY, e.ifract());
        Lines.stroke(e.fract()*2f + 0.1f);
        Lines.circle(e.x, e.y, e.ifract()*15f);
        Draw.reset();
    }),
    empshockwave = new Effect(7f, e -> {
        Draw.color(Color.WHITE, Color.SKY, e.ifract());
        Lines.stroke(e.fract()*2f);
        Lines.circle(e.x, e.y, e.ifract()*40f);
        Draw.reset();
    }),
    empspark = new Effect(13, e -> {
        Angles.randLenVectors(e.id, 7, 1f + e.ifract()*12f, (x, y)->{
            float len = 1f+e.fract()*6f;
            Draw.color(Color.SKY);
            Lines.lineAngle(e.x + x, e.y + y, Mathf.atan2(x, y), len);
            Draw.reset();
        });
    }),
    shellsmoke = new Effect(20, e -> {
        Angles.randLenVectors(e.id, 8, 3f + e.ifract()*17f, (x, y)->{
            float size = 2f+e.fract()*5f;
            Draw.color(Color.LIGHT_GRAY, Color.DARK_GRAY, e.ifract());
            Draw.rect("circle", e.x + x, e.y + y, size, size);
            Draw.reset();
        });
    }),
    shellexplosion = new Effect(9, e -> {
        Lines.stroke(2f - e.ifract()*1.7f);
        Draw.color(Color.WHITE, Color.LIGHT_GRAY, e.ifract());
        Lines.circle(e.x, e.y, 3f + e.ifract() * 9f);
        Draw.reset();
    }),
    blastexplosion = new Effect(14, e -> {
        Lines.stroke(1.2f - e.ifract());
        Draw.color(Color.WHITE, Fx.lightOrange, e.ifract());
        Lines.circle(e.x, e.y, 1.5f + e.ifract() * 9f);
        Draw.reset();
    }),
    laserhit = new Effect(10, e -> {
        Lines.stroke(1f);
        Draw.color(Color.WHITE, Color.SKY, e.ifract());
        Lines.spikes(e.x, e.y, e.ifract() * 2f, 2, 6);
        Draw.reset();
    }),
    shieldhit = new Effect(9, e -> {
        Lines.stroke(1f);
        Draw.color(Color.WHITE, Color.SKY, e.ifract());
        Lines.spikes(e.x, e.y, e.ifract() * 5f, 2, 6);
        Lines.stroke(4f*e.fract());
        Lines.circle(e.x, e.y, e.ifract()*14f);
        Draw.reset();
    }),
    laserShoot = new Effect(8, e -> {
        Draw.color(Color.WHITE, Fx.lightOrange, e.ifract());
        Shapes.lineShot(e.x, e.y, e.rotation, 3, e.fract(), 6f, 2f, 0.8f);
        Draw.reset();
    }),
    spreadShoot = new Effect(12, e -> {
        Draw.color(Color.WHITE, Color.PURPLE, e.ifract());
        Shapes.lineShot(e.x, e.y, e.rotation, 3, e.fract(), 9f, 3.5f, 0.8f);
        Draw.reset();
    }),
    clusterShoot = new Effect(12, e -> {
        Draw.color(Color.WHITE, Fx.lightOrange, e.ifract());
        Shapes.lineShot(e.x, e.y, e.rotation, 3, e.fract(), 10f, 2.5f, 0.7f);
        Draw.reset();
    }),
    vulcanShoot = new Effect(8, e -> {
        Draw.color(Fx.lighterOrange, Fx.lightOrange, e.ifract());
        Shapes.lineShot(e.x, e.y, e.rotation, 3, e.fract(), 10f, 2f, 0.7f);
        Draw.reset();
    }),
    shockShoot = new Effect(8, e -> {
        Draw.color(Color.WHITE, Color.ORANGE, e.ifract());
        Shapes.lineShot(e.x, e.y, e.rotation, 3, e.fract(), 14f, 4f, 0.8f);
        Draw.reset();
    }),
    beamShoot = new Effect(8, e -> {
        Draw.color(Fx.beamLight, Fx.beam, e.ifract());
        Shapes.lineShot(e.x, e.y, e.rotation - 70, 3, e.fract(), 12f, 1f, 0.5f);
        Shapes.lineShot(e.x, e.y, e.rotation + 70, 3, e.fract(), 12f, 1f, 0.5f);
        Draw.reset();
    }),
    beamhit = new Effect(8, e -> {
        Draw.color(Fx.beamLight, Fx.beam, e.ifract());
        Lines.stroke(e.fract()*3f+0.5f);
        Lines.circle(e.x, e.y, e.ifract()*8f);
        Lines.spikes(e.x, e.y, e.ifract()*6f, 2f, 4, 45);
        Draw.reset();
    }),
    blockexplosion = new Effect(13, e -> {
        Angles.randLenVectors(e.id+1, 8, 5f + e.ifract()*11f, (x, y)->{
            float size = 2f+e.fract()*8f;
            Draw.color(Color.LIGHT_GRAY, Color.DARK_GRAY, e.ifract());
            Draw.rect("circle", e.x + x, e.y + y, size, size);
            Draw.reset();
        });

        Lines.stroke(2f*e.fract()+0.4f);
        Draw.color(Color.WHITE, Color.ORANGE, e.powfract());
        Lines.circle(e.x, e.y, 2f + e.powfract() * 9f);

        Draw.color(e.ifract() < 0.5f ? Color.WHITE : Color.DARK_GRAY);
        Angles.randLenVectors(e.id, 5, 8f, (x, y)->{
            Fill.circle(e.x + x, e.y + y, e.fract()*5f + 1f);
        });

        Draw.reset();
    }),
    clusterbomb = new Effect(10f, e -> {
        Draw.color(Color.WHITE, Fx.lightOrange, e.ifract());
        Lines.stroke(e.fract()*1.5f);
        Lines.poly(e.x, e.y, 4, e.fract()*8f);
        Lines.circle(e.x, e.y, e.ifract()*14f);
        Draw.reset();
    }),
    railsmoke = new Effect(30, e -> {
        Draw.color(Color.LIGHT_GRAY, Color.WHITE, e.ifract());
        float size = e.fract()*4f;
        Draw.rect("circle", e.x, e.y, size, size);
        Draw.reset();
    }),
    chainsmoke = new Effect(30, e -> {
        Draw.color(Fx.lightGray);
        float size = e.fract()*4f;
        Draw.rect("circle", e.x, e.y, size, size);
        Draw.reset();
    });
}
