package io.anuke.mindustry.content.fx;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.game.ContentList;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public class BulletFx extends FxList implements ContentList{
    public static Effect hitBulletSmall, hitFuse, hitBulletBig, hitFlameSmall, hitLiquid, hitLaser, hitLancer, hitMeltdown, despawn, flakExplosion, blastExplosion, plasticExplosion,
            artilleryTrail, incendTrail, missileTrail, absorb, flakExplosionBig, plasticExplosionFlak;

    @Override
    public void load(){

        hitBulletSmall = new Effect(14, e -> {
            Draw.color(Color.WHITE, Palette.lightOrange, e.fin());

            e.scaled(7f, s -> {
                Lines.stroke(0.5f + s.fout());
                Lines.circle(e.x, e.y, s.fin()*5f);
            });


            Lines.stroke(0.5f + e.fout());

            Angles.randLenVectors(e.id, 5, e.fin() * 15f, (x, y) -> {
                float ang = Mathf.atan2(x, y);
                Lines.lineAngle(e.x + x, e.y + y, ang, e.fout() * 3 + 1f);
            });

            Draw.reset();
        });

        hitFuse = new Effect(14, e -> {
            Draw.color(Color.WHITE, Palette.surge, e.fin());

            e.scaled(7f, s -> {
                Lines.stroke(0.5f + s.fout());
                Lines.circle(e.x, e.y, s.fin()*7f);
            });


            Lines.stroke(0.5f + e.fout());

            Angles.randLenVectors(e.id, 6, e.fin() * 15f, (x, y) -> {
                float ang = Mathf.atan2(x, y);
                Lines.lineAngle(e.x + x, e.y + y, ang, e.fout() * 3 + 1f);
            });

            Draw.reset();
        });

        hitBulletBig = new Effect(13, e -> {
            Draw.color(Color.WHITE, Palette.lightOrange, e.fin());
            Lines.stroke(0.5f + e.fout() * 1.5f);

            Angles.randLenVectors(e.id, 8, e.finpow() * 30f, e.rotation, 50f, (x, y) -> {
                float ang = Mathf.atan2(x, y);
                Lines.lineAngle(e.x + x, e.y + y, ang, e.fout() * 4 + 1.5f);
            });

            Draw.reset();
        });

        hitFlameSmall = new Effect(14, e -> {
            Draw.color(Palette.lightFlame, Palette.darkFlame, e.fin());
            Lines.stroke(0.5f + e.fout());

            Angles.randLenVectors(e.id, 5, e.fin() * 15f, e.rotation, 50f, (x, y) -> {
                float ang = Mathf.atan2(x, y);
                Lines.lineAngle(e.x + x, e.y + y, ang, e.fout() * 3 + 1f);
            });

            Draw.reset();
        });

        hitLiquid = new Effect(16, e -> {
            Draw.color(e.color);

            Angles.randLenVectors(e.id, 5, e.fin() * 15f, e.rotation + 180f, 60f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 2f);
            });

            Draw.reset();
        });

        hitLancer = new Effect(12, e -> {
            Draw.color(Color.WHITE);
            Lines.stroke(e.fout() * 1.5f);

            Angles.randLenVectors(e.id, 8, e.finpow() * 17f, e.rotation, 360f, (x, y) -> {
                float ang = Mathf.atan2(x, y);
                Lines.lineAngle(e.x + x, e.y + y, ang, e.fout() * 4 + 1f);
            });

            Draw.reset();
        });

        hitMeltdown = new Effect(12, e -> {
            Draw.color(Palette.meltdownHit);
            Lines.stroke(e.fout() * 2f);

            Angles.randLenVectors(e.id, 6, e.finpow() * 18f, e.rotation, 360f, (x, y) -> {
                float ang = Mathf.atan2(x, y);
                Lines.lineAngle(e.x + x, e.y + y, ang, e.fout() * 4 + 1f);
            });

            Draw.reset();
        });

        hitLaser = new Effect(8, e -> {
            Draw.color(Color.WHITE, Palette.heal, e.fin());
            Lines.stroke(0.5f + e.fout());
            Lines.circle(e.x, e.y, e.fin()*5f);
            Draw.reset();
        });

        despawn = new Effect(12, e -> {
            Draw.color(Palette.lighterOrange, Color.GRAY, e.fin());
            Lines.stroke(e.fout());

            Angles.randLenVectors(e.id, 7, e.fin() * 7f, e.rotation, 40f, (x, y) -> {
                float ang = Mathf.atan2(x, y);
                Lines.lineAngle(e.x + x, e.y + y, ang, e.fout() * 2 + 1f);
            });

            Draw.reset();
        });

        flakExplosion = new Effect(20, e -> {

            Draw.color(Palette.bulletYellow);
            e.scaled(6, i -> {
                Lines.stroke(3f * i.fout());
                Lines.circle(e.x, e.y, 3f + i.fin() * 10f);
            });

            Draw.color(Color.GRAY);

            Angles.randLenVectors(e.id, 5, 2f + 23f * e.finpow(), (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 3f + 0.5f);
            });

            Draw.color(Palette.lighterOrange);
            Lines.stroke(1f * e.fout());

            Angles.randLenVectors(e.id + 1, 4, 1f + 23f * e.finpow(), (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.atan2(x, y), 1f + e.fout() * 3f);
            });

            Draw.reset();
        });

        plasticExplosion = new Effect(24, e -> {

            Draw.color(Palette.plastaniumFront);
            e.scaled(7, i -> {
                Lines.stroke(3f * i.fout());
                Lines.circle(e.x, e.y, 3f + i.fin() * 24f);
            });

            Draw.color(Color.GRAY);

            Angles.randLenVectors(e.id, 7, 2f + 28f * e.finpow(), (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 4f + 0.5f);
            });

            Draw.color(Palette.plastaniumBack);
            Lines.stroke(1f * e.fout());

            Angles.randLenVectors(e.id + 1, 4, 1f + 25f * e.finpow(), (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.atan2(x, y), 1f + e.fout() * 3f);
            });

            Draw.reset();
        });

        plasticExplosionFlak = new Effect(28, e -> {

            Draw.color(Palette.plastaniumFront);
            e.scaled(7, i -> {
                Lines.stroke(3f * i.fout());
                Lines.circle(e.x, e.y, 3f + i.fin() * 34f);
            });

            Draw.color(Color.GRAY);

            Angles.randLenVectors(e.id, 7, 2f + 30f * e.finpow(), (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 4f + 0.5f);
            });

            Draw.color(Palette.plastaniumBack);
            Lines.stroke(1f * e.fout());

            Angles.randLenVectors(e.id + 1, 4, 1f + 30f * e.finpow(), (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.atan2(x, y), 1f + e.fout() * 3f);
            });

            Draw.reset();
        });

        blastExplosion = new Effect(22, e -> {

            Draw.color(Palette.missileYellow);
            e.scaled(6, i -> {
                Lines.stroke(3f * i.fout());
                Lines.circle(e.x, e.y, 3f + i.fin() * 15f);
            });

            Draw.color(Color.GRAY);

            Angles.randLenVectors(e.id, 5, 2f + 23f * e.finpow(), (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 4f + 0.5f);
            });

            Draw.color(Palette.missileYellowBack);
            Lines.stroke(1f * e.fout());

            Angles.randLenVectors(e.id + 1, 4, 1f + 23f * e.finpow(), (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.atan2(x, y), 1f + e.fout() * 3f);
            });

            Draw.reset();
        });

        artilleryTrail = new Effect(50, e -> {
            Draw.color(e.color);
            Fill.circle(e.x, e.y, e.rotation * e.fout());
            Draw.reset();
        });

        incendTrail = new Effect(50, e -> {
            Draw.color(Palette.lightOrange);
            Fill.circle(e.x, e.y, e.rotation * e.fout());
            Draw.reset();
        });

        missileTrail = new Effect(50, e -> {
            Draw.color(e.color);
            Fill.circle(e.x, e.y, e.rotation * e.fout());
            Draw.reset();
        });

        absorb = new Effect(12, e -> {
            Draw.color(Palette.accent);
            Lines.stroke(2f * e.fout());
            Lines.circle(e.x, e.y, 5f * e.fout());
            Draw.reset();
        });

        flakExplosionBig = new Effect(30, e -> {

            Draw.color(Palette.bulletYellowBack);
            e.scaled(6, i -> {
                Lines.stroke(3f * i.fout());
                Lines.circle(e.x, e.y, 3f + i.fin() * 25f);
            });

            Draw.color(Color.GRAY);

            Angles.randLenVectors(e.id, 6, 2f + 23f * e.finpow(), (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 4f + 0.5f);
            });

            Draw.color(Palette.bulletYellow);
            Lines.stroke(1f * e.fout());

            Angles.randLenVectors(e.id + 1, 4, 1f + 23f * e.finpow(), (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.atan2(x, y), 1f + e.fout() * 3f);
            });

            Draw.reset();
        });
    }

}
