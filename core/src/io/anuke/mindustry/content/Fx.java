package io.anuke.mindustry.content;

import io.anuke.arc.Core;
import io.anuke.arc.entities.Effects.Effect;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Fill;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.math.Angles;
import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.entities.effect.GroundEffectEntity.GroundEffect;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shapes;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.Item;

import static io.anuke.mindustry.Vars.tilesize;

public class Fx implements ContentList{
    public static Effect

    none, placeBlock, breakBlock, smoke, spawn, tapBlock, select,
    vtolHover, unitDrop, unitPickup, unitLand, pickup, healWave, heal, landShock, reactorsmoke, nuclearsmoke, nuclearcloud,
    redgeneratespark, generatespark, fuelburn, plasticburn, pulverize, pulverizeRed, pulverizeRedder, pulverizeSmall, pulverizeMedium,
    producesmoke, smeltsmoke, formsmoke, blastsmoke, lava, doorclose, dooropenlarge, doorcloselarge, purify, purifyoil, purifystone, generate,
    mine, mineBig, mineHuge, smelt, teleportActivate, teleport, teleportOut, ripple, bubble, commandSend,
    healBlock, healBlockFull, healWaveMend, overdriveWave, overdriveBlockFull, shieldBreak, hitBulletSmall, hitFuse,
    hitBulletBig, hitFlameSmall, hitLiquid, hitLaser, hitLancer, hitMeltdown, despawn, flakExplosion, blastExplosion,
    plasticExplosion, artilleryTrail, incendTrail, missileTrail, absorb, flakExplosionBig, plasticExplosionFlak, burning, fire,
    fireSmoke, steam, fireballsmoke, ballfire, freezing, melting, wet, oily, overdriven, dropItem, shockwave,
    bigShockwave, nuclearShockwave, explosion, blockExplosion, blockExplosionSmoke, shootSmall, shootHeal, shootSmallSmoke, shootBig, shootBig2, shootBigSmoke,
    shootBigSmoke2, shootSmallFlame, shootLiquid, shellEjectSmall, shellEjectMedium,
    shellEjectBig, lancerLaserShoot, lancerLaserShootSmoke, lancerLaserCharge, lancerLaserChargeBegin, lightningCharge, lightningShoot;

    @Override
    public void load(){

        none = new Effect(0, 0f, e -> {});

        placeBlock = new Effect(16, e -> {
            Draw.color(Palette.accent);
            Lines.stroke(3f - e.fin() * 2f);
            Lines.square(e.x, e.y, tilesize / 2f * e.rotation + e.fin() * 3f);
            Draw.reset();
        });

        tapBlock = new Effect(12, e -> {
            Draw.color(Palette.accent);
            Lines.stroke(3f - e.fin() * 2f);
            Lines.circle(e.x, e.y, 4f + (tilesize / 1.5f * e.rotation) * e.fin());
            Draw.reset();
        });

        breakBlock = new Effect(12, e -> {
            Draw.color(Palette.remove);
            Lines.stroke(3f - e.fin() * 2f);
            Lines.square(e.x, e.y, tilesize / 2f * e.rotation + e.fin() * 3f);

            Angles.randLenVectors(e.id, 3 + (int) (e.rotation * 3), e.rotation * 2f + (tilesize * e.rotation) * e.finpow(), (x, y) -> {
                Fill.square(e.x + x, e.y + y, 1f + e.fout() * (3f + e.rotation));
            });
            Draw.reset();
        });

        select = new Effect(23, e -> {
            Draw.color(Palette.accent);
            Lines.stroke(e.fout() * 3f);
            Lines.circle(e.x, e.y, 3f + e.fin() * 14f);
            Draw.reset();
        });

        smoke = new Effect(100, e -> {
            Draw.color(Color.GRAY, Palette.darkishGray, e.fin());
            float size = 7f - e.fin() * 7f;
            Draw.rect("circle", e.x, e.y, size, size);
            Draw.reset();
        });

        spawn = new Effect(23, e -> {
            Lines.stroke(2f * e.fout());
            Draw.color(Palette.accent);
            Lines.poly(e.x, e.y, 4, 3f + e.fin() * 8f);
            Draw.reset();
        });
        
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

        landShock = new GroundEffect(12, e -> {
            Draw.color(Palette.lancerLaser);
            Lines.stroke(e.fout() * 3f);
            Lines.poly(e.x, e.y, 12, 20f * e.fout());
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

        heal = new Effect(11, e -> {
            Draw.color(Palette.heal);
            Lines.stroke(e.fout() * 2f);
            Lines.poly(e.x, e.y, 10, 2f + e.finpow() * 7f);
            Draw.color();
        });


        hitBulletSmall = new Effect(14, e -> {
            Draw.color(Color.WHITE, Palette.lightOrange, e.fin());

            e.scaled(7f, s -> {
                Lines.stroke(0.5f + s.fout());
                Lines.circle(e.x, e.y, s.fin()*5f);
            });


            Lines.stroke(0.5f + e.fout());

            Angles.randLenVectors(e.id, 5, e.fin() * 15f, (x, y) -> {
                float ang = Mathf.angle(x, y);
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
                float ang = Mathf.angle(x, y);
                Lines.lineAngle(e.x + x, e.y + y, ang, e.fout() * 3 + 1f);
            });

            Draw.reset();
        });

        hitBulletBig = new Effect(13, e -> {
            Draw.color(Color.WHITE, Palette.lightOrange, e.fin());
            Lines.stroke(0.5f + e.fout() * 1.5f);

            Angles.randLenVectors(e.id, 8, e.finpow() * 30f, e.rotation, 50f, (x, y) -> {
                float ang = Mathf.angle(x, y);
                Lines.lineAngle(e.x + x, e.y + y, ang, e.fout() * 4 + 1.5f);
            });

            Draw.reset();
        });

        hitFlameSmall = new Effect(14, e -> {
            Draw.color(Palette.lightFlame, Palette.darkFlame, e.fin());
            Lines.stroke(0.5f + e.fout());

            Angles.randLenVectors(e.id, 5, e.fin() * 15f, e.rotation, 50f, (x, y) -> {
                float ang = Mathf.angle(x, y);
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
                float ang = Mathf.angle(x, y);
                Lines.lineAngle(e.x + x, e.y + y, ang, e.fout() * 4 + 1f);
            });

            Draw.reset();
        });

        hitMeltdown = new Effect(12, e -> {
            Draw.color(Palette.meltdownHit);
            Lines.stroke(e.fout() * 2f);

            Angles.randLenVectors(e.id, 6, e.finpow() * 18f, e.rotation, 360f, (x, y) -> {
                float ang = Mathf.angle(x, y);
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
                float ang = Mathf.angle(x, y);
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
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
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
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
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
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
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
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
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
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
            });

            Draw.reset();
        });


        burning = new Effect(35f, e -> {
            Draw.color(Palette.lightFlame, Palette.darkFlame, e.fin());

            Angles.randLenVectors(e.id, 3, 2f + e.fin() * 7f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, 0.1f + e.fout() * 1.4f);
            });

            Draw.color();
        });

        fire = new Effect(35f, e -> {
            Draw.color(Palette.lightFlame, Palette.darkFlame, e.fin());

            Angles.randLenVectors(e.id, 2, 2f + e.fin() * 7f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, 0.2f + e.fslope() * 1.5f);
            });

            Draw.color();
        });

        fireSmoke = new Effect(35f, e -> {
            Draw.color(Color.GRAY);

            Angles.randLenVectors(e.id, 1, 2f + e.fin() * 7f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, 0.2f + e.fslope() * 1.5f);
            });

            Draw.color();
        });

        steam = new Effect(35f, e -> {
            Draw.color(Color.LIGHT_GRAY);

            Angles.randLenVectors(e.id, 2, 2f + e.fin() * 7f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, 0.2f + e.fslope() * 1.5f);
            });

            Draw.color();
        });

        fireballsmoke = new Effect(25f, e -> {
            Draw.color(Color.GRAY);

            Angles.randLenVectors(e.id, 1, 2f + e.fin() * 7f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, 0.2f + e.fout() * 1.5f);
            });

            Draw.color();
        });

        ballfire = new Effect(25f, e -> {
            Draw.color(Palette.lightFlame, Palette.darkFlame, e.fin());

            Angles.randLenVectors(e.id, 2, 2f + e.fin() * 7f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, 0.2f + e.fout() * 1.5f);
            });

            Draw.color();
        });

        freezing = new Effect(40f, e -> {
            Draw.color(Liquids.cryofluid.color);

            Angles.randLenVectors(e.id, 2, 1f + e.fin() * 2f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 1.2f);
            });

            Draw.color();
        });

        melting = new Effect(40f, e -> {
            Draw.color(Liquids.lava.color, Color.WHITE, e.fout() / 5f + Mathf.randomSeedRange(e.id, 0.12f));

            Angles.randLenVectors(e.id, 2, 1f + e.fin() * 3f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, .2f + e.fout() * 1.2f);
            });

            Draw.color();
        });

        wet = new Effect(40f, e -> {
            Draw.color(Liquids.water.color);

            Angles.randLenVectors(e.id, 2, 1f + e.fin() * 2f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 1f);
            });

            Draw.color();
        });

        oily = new Effect(42f, e -> {
            Draw.color(Liquids.oil.color);

            Angles.randLenVectors(e.id, 2, 1f + e.fin() * 2f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 1f);
            });

            Draw.color();
        });

        overdriven = new Effect(20f, e -> {
            Draw.color(Palette.accent);

            Angles.randLenVectors(e.id, 2, 1f + e.fin() * 2f, (x, y) -> {
                Fill.square(e.x + x, e.y + y, e.fout() * 2.3f+0.5f);
            });

            Draw.color();
        });

        dropItem = new Effect(20f, e -> {
            float length = 20f * e.finpow();
            float size = 7f * e.fout();

            Draw.rect(((Item) e.data).region, e.x + Angles.trnsx(e.rotation, length), e.y + Angles.trnsy(e.rotation, length), size, size);
        });


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
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
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
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
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
            Fill.rect(e.x + Angles.trnsx(lr, len) + Mathf.randomSeedRange(e.id + i + 7, 3f * e.fin()),
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
                Draw.rect(Core.atlas.find("casing"),
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
                Draw.rect(Core.atlas.find("casing"),
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
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fout() * 9f);
            });

            Draw.reset();
        });

        lancerLaserCharge = new Effect(38f, e -> {
            Draw.color(Palette.lancerLaser);

            Angles.randLenVectors(e.id, 2, 1f + 20f * e.fout(), e.rotation, 120f, (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fslope() * 3f + 1f);
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
                Shapes.tri(e.x + x, e.y + y, e.fslope() * 3f + 1, e.fslope() * 3f + 1, Mathf.angle(x, y));
            });

            Draw.reset();
        });

        lightningShoot = new Effect(12f, e -> {
            Draw.color(Color.WHITE, Palette.lancerLaser, e.fin());
            Lines.stroke(e.fout() * 1.2f + 0.5f);

            Angles.randLenVectors(e.id, 7, 25f * e.finpow(), e.rotation, 50f, (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fin() * 5f + 2f);
            });

            Draw.reset();
        });
    }

    @Override
    public ContentType type(){
        return ContentType.effect;
    }
}
