package io.anuke.mindustry.content.fx;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.entities.effect.GroundEffectEntity.GroundEffect;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.game.ContentList;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.graphics.Shapes;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public class ShootFx extends FxList implements ContentList{
    public static Effect shootSmall, shootHeal, shootSmallSmoke, shootBig, shootBig2, shootBigSmoke, shootBigSmoke2, shootSmallFlame, shootLiquid, shellEjectSmall, shellEjectMedium, shellEjectBig, lancerLaserShoot, lancerLaserShootSmoke, lancerLaserCharge, lancerLaserChargeBegin, lightningCharge, lightningShoot;

    @Override
    public void load(){

        shootSmall = new Effect(8, e -> {
            Draw.color(Palette.lighterOrange, Palette.lightOrange, e.fin());
            float w = 1f + 5 * e.fout();
            Shapes.tri(e.x, e.y, w, 15f * e.fout(), e.rotation);
            Shapes.tri(e.x, e.y, w, 3f * e.fout(), e.rotation + 180f);
            Draw.reset();
        });

        shootHeal = new Effect(8, e -> {
            Draw.color(Palette.heal);
            float w = 1f + 5 * e.fout();
            Shapes.tri(e.x, e.y, w, 17f * e.fout(), e.rotation);
            Shapes.tri(e.x, e.y, w, 4f * e.fout(), e.rotation + 180f);
            Draw.reset();
        });

        shootSmallSmoke = new Effect(20f, e -> {
            Draw.color(Palette.lighterOrange, Color.LIGHT_GRAY, Color.GRAY, e.fin());

            Angles.randLenVectors(e.id, 5, e.finpow() * 6f, e.rotation, 20f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 1.5f);
            });

            Draw.reset();
        });

        shootBig = new Effect(9, e -> {
            Draw.color(Palette.lighterOrange, Palette.lightOrange, e.fin());
            float w = 1.2f + 7 * e.fout();
            Shapes.tri(e.x, e.y, w, 25f * e.fout(), e.rotation);
            Shapes.tri(e.x, e.y, w, 4f * e.fout(), e.rotation + 180f);
            Draw.reset();
        });

        shootBig2 = new Effect(10, e -> {
            Draw.color(Palette.lightOrange, Color.GRAY, e.fin());
            float w = 1.2f + 8 * e.fout();
            Shapes.tri(e.x, e.y, w, 29f * e.fout(), e.rotation);
            Shapes.tri(e.x, e.y, w, 5f * e.fout(), e.rotation + 180f);
            Draw.reset();
        });

        shootBigSmoke = new Effect(17f, e -> {
            Draw.color(Palette.lighterOrange, Color.LIGHT_GRAY, Color.GRAY, e.fin());

            Angles.randLenVectors(e.id, 8, e.finpow() * 19f, e.rotation, 10f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 2f + 0.2f);
            });

            Draw.reset();
        });

        shootBigSmoke2 = new Effect(18f, e -> {
            Draw.color(Palette.lightOrange, Color.LIGHT_GRAY, Color.GRAY, e.fin());

            Angles.randLenVectors(e.id, 9, e.finpow() * 23f, e.rotation, 20f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 2.4f + 0.2f);
            });

            Draw.reset();
        });

        shootSmallFlame = new Effect(30f, e -> {
            Draw.color(Palette.lightFlame, Palette.darkFlame, Color.GRAY, e.fin());

            Angles.randLenVectors(e.id, 8, e.finpow() * 36f, e.rotation, 10f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, 0.65f + e.fout() * 1.5f);
            });

            Draw.reset();
        });

        shootLiquid = new Effect(40f, e -> {
            Draw.color(e.color, Color.WHITE, e.fout() / 6f + Mathf.randomSeedRange(e.id, 0.1f));

            Angles.randLenVectors(e.id, 6, e.finpow() * 60f, e.rotation, 11f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, 0.5f + e.fout() * 2.5f);
            });

            Draw.reset();
        });

        shellEjectSmall = new GroundEffect(30f, 400f, e -> {
            Draw.color(Palette.lightOrange, Color.LIGHT_GRAY, Palette.lightishGray, e.fin());
            float rot = Math.abs(e.rotation) + 90f;

            int i = Mathf.sign(e.rotation);

            float len = (2f + e.finpow() * 6f) * i;
            float lr = rot + e.fin() * 30f * i;
            Draw.rect("white",
                    e.x + Angles.trnsx(lr, len) + Mathf.randomSeedRange(e.id + i + 7, 3f * e.fin()),
                    e.y + Angles.trnsy(lr, len) + Mathf.randomSeedRange(e.id + i + 8, 3f * e.fin()),
                    1f, 2f, rot + e.fin() * 50f * i);

            Draw.color();
        });

        shellEjectMedium = new GroundEffect(34f, 400f, e -> {
            Draw.color(Palette.lightOrange, Color.LIGHT_GRAY, Palette.lightishGray, e.fin());
            float rot = e.rotation + 90f;
            for(int i : Mathf.signs){
                float len = (2f + e.finpow() * 10f) * i;
                float lr = rot + e.fin() * 20f * i;
                Draw.rect("casing",
                        e.x + Angles.trnsx(lr, len) + Mathf.randomSeedRange(e.id + i + 7, 3f * e.fin()),
                        e.y + Angles.trnsy(lr, len) + Mathf.randomSeedRange(e.id + i + 8, 3f * e.fin()),
                        2f, 3f, rot);
            }

            Draw.color(Color.LIGHT_GRAY, Color.GRAY, e.fin());

            for(int i : Mathf.signs){
                Angles.randLenVectors(e.id, 4, 1f + e.finpow() * 11f, e.rotation + 90f * i, 20f, (x, y) -> {
                    Fill.circle(e.x + x, e.y + y, e.fout() * 1.5f);
                });
            }

            Draw.color();
        });

        shellEjectBig = new GroundEffect(22f, 400f, e -> {
            Draw.color(Palette.lightOrange, Color.LIGHT_GRAY, Palette.lightishGray, e.fin());
            float rot = e.rotation + 90f;
            for(int i : Mathf.signs){
                float len = (4f + e.finpow() * 8f) * i;
                float lr = rot + Mathf.randomSeedRange(e.id + i + 6, 20f * e.fin()) * i;
                Draw.rect("casing",
                        e.x + Angles.trnsx(lr, len) + Mathf.randomSeedRange(e.id + i + 7, 3f * e.fin()),
                        e.y + Angles.trnsy(lr, len) + Mathf.randomSeedRange(e.id + i + 8, 3f * e.fin()),
                        2.5f, 4f,
                        rot + e.fin() * 30f * i + Mathf.randomSeedRange(e.id + i + 9, 40f * e.fin()));
            }

            Draw.color(Color.LIGHT_GRAY);

            for(int i : Mathf.signs){
                Angles.randLenVectors(e.id, 4, -e.finpow() * 15f, e.rotation + 90f * i, 25f, (x, y) -> {
                    Fill.circle(e.x + x, e.y + y, e.fout() * 2f);
                });
            }

            Draw.color();
        });

        lancerLaserShoot = new Effect(21f, e -> {
            Draw.color(Palette.lancerLaser);

            for(int i : Mathf.signs){
                Shapes.tri(e.x, e.y, 4f * e.fout(), 29f, e.rotation + 90f * i);
            }

            Draw.reset();
        });

        lancerLaserShootSmoke = new Effect(26f, e -> {
            Draw.color(Palette.lancerLaser);

            Angles.randLenVectors(e.id, 7, 80f, e.rotation, 0f, (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.atan2(x, y), e.fout() * 9f);
            });

            Draw.reset();
        });

        lancerLaserCharge = new Effect(38f, e -> {
            Draw.color(Palette.lancerLaser);

            Angles.randLenVectors(e.id, 2, 1f + 20f * e.fout(), e.rotation, 120f, (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.atan2(x, y), e.fslope() * 3f + 1f);
            });

            Draw.reset();
        });

        lancerLaserChargeBegin = new Effect(71f, e -> {
            Draw.color(Palette.lancerLaser);
            Fill.circle(e.x, e.y, e.fin() * 3f);

            Draw.color();
            Fill.circle(e.x, e.y, e.fin() * 2f);
        });

        lightningCharge = new Effect(38f, e -> {
            Draw.color(Palette.lancerLaser);

            Angles.randLenVectors(e.id, 2, 1f + 20f * e.fout(), e.rotation, 120f, (x, y) -> {
                Shapes.tri(e.x + x, e.y + y, e.fslope() * 3f + 1, e.fslope() * 3f + 1, Mathf.atan2(x, y));
            });

            Draw.reset();
        });

        lightningShoot = new Effect(12f, e -> {
            Draw.color(Color.WHITE, Palette.lancerLaser, e.fin());
            Lines.stroke(e.fout() * 1.2f + 0.5f);

            Angles.randLenVectors(e.id, 7, 25f * e.finpow(), e.rotation, 50f, (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.atan2(x, y), e.fin() * 5f + 2f);
            });

            Draw.reset();
        });
    }
}
