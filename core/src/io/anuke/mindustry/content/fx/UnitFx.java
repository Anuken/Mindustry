package io.anuke.mindustry.content.fx;

import io.anuke.mindustry.entities.effect.GroundEffectEntity.GroundEffect;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.game.ContentList;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public class UnitFx extends FxList implements ContentList{
    public static Effect vtolHover, unitDrop, unitPickup, unitLand, pickup, healWave, healWaveMend, heal;

    @Override
    public void load(){

        vtolHover = new Effect(40f, e -> {
            float len = e.finpow() * 10f;
            float ang = e.rotation + Mathf.randomSeedRange(e.id, 30f);
            Draw.color(Palette.lightFlame, Palette.lightOrange, e.fin());
            Fill.circle(e.x + Angles.trnsx(ang, len), e.y + Angles.trnsy(ang, len), 2f * e.fout());
            Draw.reset();
        });

        unitDrop = new GroundEffect(30, e -> {
            Draw.color(Palette.lightishGray);
            Angles.randLenVectors(e.id, 9, 3 + 20f * e.finpow(), (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 4f + 0.4f);
            });
            Draw.reset();
        });

        unitLand = new GroundEffect(30, e -> {
            Draw.color(Palette.lightishGray, e.color, e.rotation);
            Angles.randLenVectors(e.id, 6, 17f * e.finpow(), (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 4f + 0.3f);
            });
            Draw.reset();
        });

        unitPickup = new GroundEffect(18, e -> {
            Draw.color(Palette.lightishGray);
            Lines.stroke(e.fin() * 2f);
            Lines.poly(e.x, e.y, 4, 13f * e.fout());
            Draw.reset();
        });

        pickup = new Effect(18, e -> {
            Draw.color(Palette.lightishGray);
            Lines.stroke(e.fout() * 2f);
            Lines.spikes(e.x, e.y, 1f + e.fin() * 6f, e.fout() * 4f, 6);
            Draw.reset();
        });

        healWave = new Effect(22, e -> {
            Draw.color(Palette.heal);
            Lines.stroke(e.fout() * 2f);
            Lines.poly(e.x, e.y, 30, 4f + e.finpow() * 60f);
            Draw.color();
        });

        healWaveMend = new Effect(40, e -> {
            Draw.color(e.color);
            Lines.stroke(e.fout() * 2f);
            Lines.poly(e.x, e.y, 30, e.finpow() * e.rotation);
            Draw.color();
        });

        heal = new Effect(11, e -> {
            Draw.color(Palette.heal);
            Lines.stroke(e.fout() * 2f);
            Lines.poly(e.x, e.y, 10, 2f + e.finpow() * 7f);
            Draw.color();
        });
    }
}
