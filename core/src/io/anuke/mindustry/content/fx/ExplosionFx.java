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

public class ExplosionFx extends FxList implements ContentList{
    public static Effect shockwave, bigShockwave, nuclearShockwave, explosion, blockExplosion, blockExplosionSmoke;

    @Override
    public void load(){

        shockwave = new Effect(10f, 80f, e -> {
            Draw.color(Color.WHITE, Color.LIGHT_GRAY, e.fin());
            Lines.stroke(e.fout() * 2f + 0.2f);
            Lines.circle(e.x, e.y, e.fin() * 28f);
            Draw.reset();
        });

        bigShockwave = new Effect(10f, 80f, e -> {
            Draw.color(Color.WHITE, Color.LIGHT_GRAY, e.fin());
            Lines.stroke(e.fout() * 3f);
            Lines.circle(e.x, e.y, e.fin() * 50f);
            Draw.reset();
        });

        nuclearShockwave = new Effect(10f, 200f, e -> {
            Draw.color(Color.WHITE, Color.LIGHT_GRAY, e.fin());
            Lines.stroke(e.fout() * 3f + 0.2f);
            Lines.poly(e.x, e.y, 40, e.fin() * 140f);
            Draw.reset();
        });

        explosion = new Effect(30, e -> {
            e.scaled(7, i -> {
                Lines.stroke(3f * i.fout());
                Lines.circle(e.x, e.y, 3f + i.fin() * 10f);
            });

            Draw.color(Color.GRAY);

            Angles.randLenVectors(e.id, 6, 2f + 19f * e.finpow(), (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 3f + 0.5f);
                Fill.circle(e.x + x / 2f, e.y + y / 2f, e.fout() * 1f);
            });

            Draw.color(Palette.lighterOrange, Palette.lightOrange, Color.GRAY, e.fin());
            Lines.stroke(1.5f * e.fout());

            Angles.randLenVectors(e.id + 1, 8, 1f + 23f * e.finpow(), (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.atan2(x, y), 1f + e.fout() * 3f);
            });

            Draw.reset();
        });

        blockExplosion = new Effect(30, e -> {
            e.scaled(7, i -> {
                Lines.stroke(3.1f * i.fout());
                Lines.circle(e.x, e.y, 3f + i.fin() * 14f);
            });

            Draw.color(Color.GRAY);

            Angles.randLenVectors(e.id, 6, 2f + 19f * e.finpow(), (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 3f + 0.5f);
                Fill.circle(e.x + x / 2f, e.y + y / 2f, e.fout() * 1f);
            });

            Draw.color(Palette.lighterOrange, Palette.lightOrange, Color.GRAY, e.fin());
            Lines.stroke(1.7f * e.fout());

            Angles.randLenVectors(e.id + 1, 9, 1f + 23f * e.finpow(), (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.atan2(x, y), 1f + e.fout() * 3f);
            });

            Draw.reset();
        });

        blockExplosionSmoke = new Effect(30, e -> {
            Draw.color(Color.GRAY);

            Angles.randLenVectors(e.id, 6, 4f + 30f * e.finpow(), (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 3f);
                Fill.circle(e.x + x / 2f, e.y + y / 2f, e.fout() * 1f);
            });

            Draw.reset();
        });
    }
}
