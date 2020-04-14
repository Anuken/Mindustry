package mindustry.content;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.ctype.ContentList;
import mindustry.entities.Effects.*;
import mindustry.entities.effect.GroundEffectEntity.*;
import mindustry.entities.type.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.Cicon;

import static mindustry.Vars.*;

public class Fx implements ContentList{
    public static Effect

    none, placeBlock, breakBlock, smoke, spawn, tapBlock, select,
    vtolHover, unitDrop, unitPickup, unitLand, pickup, healWave, heal, landShock, reactorsmoke, nuclearsmoke, nuclearcloud,
    redgeneratespark, generatespark, fuelburn, plasticburn, pulverize, pulverizeRed, pulverizeRedder, pulverizeSmall, pulverizeMedium,
    producesmoke, smeltsmoke, formsmoke, blastsmoke, lava, doorclose, dooropen, dooropenlarge, doorcloselarge, purify, purifyoil, purifystone, generate,
    mine, mineBig, mineHuge, smelt, teleportActivate, teleport, teleportOut, ripple, bubble, launch,
    healBlock, healBlockFull, healWaveMend, overdriveWave, overdriveBlockFull, shieldBreak, hitBulletSmall, hitFuse,
    hitBulletBig, hitFlameSmall, hitLiquid, hitLaser, hitLancer, hitMeltdown, despawn, flakExplosion, blastExplosion,
    plasticExplosion, artilleryTrail, incendTrail, missileTrail, absorb, flakExplosionBig, plasticExplosionFlak, burning, fire,
    fireSmoke, steam, fireballsmoke, ballfire, freezing, melting, wet, oily, overdriven, dropItem, shockwave,
    bigShockwave, nuclearShockwave, explosion, blockExplosion, blockExplosionSmoke, shootSmall, shootHeal, shootSmallSmoke, shootBig, shootBig2, shootBigSmoke,
    shootBigSmoke2, shootSmallFlame, shootPyraFlame, shootLiquid, shellEjectSmall, shellEjectMedium,
    shellEjectBig, lancerLaserShoot, lancerLaserShootSmoke, lancerLaserCharge, lancerLaserChargeBegin, lightningCharge, lightningShoot,
    unitSpawn, spawnShockwave, magmasmoke, impactShockwave, impactcloud, impactsmoke, dynamicExplosion, padlaunch, commandSend, coreLand;

    @Override
    public void load(){

        none = new Effect(0, 0f, e -> {});

        unitSpawn = new Effect(30f, e -> {
            if(!(e.data instanceof BaseUnit)) return;

            Draw.alpha(e.fin());

            float scl = 1f + e.fout() * 2f;

            BaseUnit unit = (BaseUnit)e.data;
            Draw.rect(unit.getIconRegion(), e.x, e.y,
            unit.getIconRegion().getWidth() * Draw.scl * scl, unit.getIconRegion().getWidth() * Draw.scl * scl, 180f);

        });

        commandSend = new Effect(28, e -> {
            Draw.color(Pal.command);
            Lines.stroke(e.fout() * 2f);
            Lines.circle(e.x, e.y, 4f + e.finpow() * 120f);
        });

        placeBlock = new Effect(16, e -> {
            Draw.color(Pal.accent);
            Lines.stroke(3f - e.fin() * 2f);
            Lines.square(e.x, e.y, tilesize / 2f * e.rotation + e.fin() * 3f);
        });

        tapBlock = new Effect(12, e -> {
            Draw.color(Pal.accent);
            Lines.stroke(3f - e.fin() * 2f);
            Lines.circle(e.x, e.y, 4f + (tilesize / 1.5f * e.rotation) * e.fin());
        });

        breakBlock = new Effect(12, e -> {
            Draw.color(Pal.remove);
            Lines.stroke(3f - e.fin() * 2f);
            Lines.square(e.x, e.y, tilesize / 2f * e.rotation + e.fin() * 3f);

            Angles.randLenVectors(e.id, 3 + (int)(e.rotation * 3), e.rotation * 2f + (tilesize * e.rotation) * e.finpow(), (x, y) -> {
                Fill.square(e.x + x, e.y + y, 1f + e.fout() * (3f + e.rotation));
            });
        });

        select = new Effect(23, e -> {
            Draw.color(Pal.accent);
            Lines.stroke(e.fout() * 3f);
            Lines.circle(e.x, e.y, 3f + e.fin() * 14f);
        });

        smoke = new Effect(100, e -> {
            Draw.color(Color.gray, Pal.darkishGray, e.fin());
            float size = 7f - e.fin() * 7f;
            Draw.rect("circle", e.x, e.y, size, size);
        });

        magmasmoke = new Effect(110, e -> {
            Draw.color(Color.gray);
            Fill.circle(e.x, e.y, e.fslope() * 6f);
        });

        spawn = new Effect(30, e -> {
            Lines.stroke(2f * e.fout());
            Draw.color(Pal.accent);
            Lines.poly(e.x, e.y, 4, 5f + e.fin() * 12f);
        });

        padlaunch = new Effect(10, e -> {
            Lines.stroke(4f * e.fout());
            Draw.color(Pal.accent);
            Lines.poly(e.x, e.y, 4, 5f + e.fin() * 60f);
        });

        vtolHover = new Effect(40f, e -> {
            float len = e.finpow() * 10f;
            float ang = e.rotation + Mathf.randomSeedRange(e.id, 30f);
            Draw.color(Pal.lightFlame, Pal.lightOrange, e.fin());
            Fill.circle(e.x + Angles.trnsx(ang, len), e.y + Angles.trnsy(ang, len), 2f * e.fout());
        });

        unitDrop = new GroundEffect(30, e -> {
            Draw.color(Pal.lightishGray);
            Angles.randLenVectors(e.id, 9, 3 + 20f * e.finpow(), (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 4f + 0.4f);
            });
        });

        unitLand = new GroundEffect(30, e -> {
            Draw.color(Tmp.c1.set(e.color).mul(1.1f));
            Angles.randLenVectors(e.id, 6, 17f * e.finpow(), (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 4f + 0.3f);
            });
        });

        unitPickup = new GroundEffect(18, e -> {
            Draw.color(Pal.lightishGray);
            Lines.stroke(e.fin() * 2f);
            Lines.poly(e.x, e.y, 4, 13f * e.fout());
        });

        landShock = new GroundEffect(12, e -> {
            Draw.color(Pal.lancerLaser);
            Lines.stroke(e.fout() * 3f);
            Lines.poly(e.x, e.y, 12, 20f * e.fout());
        });

        pickup = new Effect(18, e -> {
            Draw.color(Pal.lightishGray);
            Lines.stroke(e.fout() * 2f);
            Lines.spikes(e.x, e.y, 1f + e.fin() * 6f, e.fout() * 4f, 6);
        });

        healWave = new Effect(22, e -> {
            Draw.color(Pal.heal);
            Lines.stroke(e.fout() * 2f);
            Lines.circle(e.x, e.y, 4f + e.finpow() * 60f);
        });

        heal = new Effect(11, e -> {
            Draw.color(Pal.heal);
            Lines.stroke(e.fout() * 2f);
            Lines.circle(e.x, e.y, 2f + e.finpow() * 7f);
        });


        hitBulletSmall = new Effect(14, e -> {
            Draw.color(Color.white, Pal.lightOrange, e.fin());

            e.scaled(7f, s -> {
                Lines.stroke(0.5f + s.fout());
                Lines.circle(e.x, e.y, s.fin() * 5f);
            });


            Lines.stroke(0.5f + e.fout());

            Angles.randLenVectors(e.id, 5, e.fin() * 15f, (x, y) -> {
                float ang = Mathf.angle(x, y);
                Lines.lineAngle(e.x + x, e.y + y, ang, e.fout() * 3 + 1f);
            });

        });

        hitFuse = new Effect(14, e -> {
            Draw.color(Color.white, Pal.surge, e.fin());

            e.scaled(7f, s -> {
                Lines.stroke(0.5f + s.fout());
                Lines.circle(e.x, e.y, s.fin() * 7f);
            });


            Lines.stroke(0.5f + e.fout());

            Angles.randLenVectors(e.id, 6, e.fin() * 15f, (x, y) -> {
                float ang = Mathf.angle(x, y);
                Lines.lineAngle(e.x + x, e.y + y, ang, e.fout() * 3 + 1f);
            });

        });

        hitBulletBig = new Effect(13, e -> {
            Draw.color(Color.white, Pal.lightOrange, e.fin());
            Lines.stroke(0.5f + e.fout() * 1.5f);

            Angles.randLenVectors(e.id, 8, e.finpow() * 30f, e.rotation, 50f, (x, y) -> {
                float ang = Mathf.angle(x, y);
                Lines.lineAngle(e.x + x, e.y + y, ang, e.fout() * 4 + 1.5f);
            });

        });

        hitFlameSmall = new Effect(14, e -> {
            Draw.color(Pal.lightFlame, Pal.darkFlame, e.fin());
            Lines.stroke(0.5f + e.fout());

            Angles.randLenVectors(e.id, 2, e.fin() * 15f, e.rotation, 50f, (x, y) -> {
                float ang = Mathf.angle(x, y);
                Lines.lineAngle(e.x + x, e.y + y, ang, e.fout() * 3 + 1f);
            });

        });

        hitLiquid = new Effect(16, e -> {
            Draw.color(e.color);

            Angles.randLenVectors(e.id, 5, e.fin() * 15f, e.rotation + 180f, 60f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 2f);
            });

        });

        hitLancer = new Effect(12, e -> {
            Draw.color(Color.white);
            Lines.stroke(e.fout() * 1.5f);

            Angles.randLenVectors(e.id, 8, e.finpow() * 17f, e.rotation, 360f, (x, y) -> {
                float ang = Mathf.angle(x, y);
                Lines.lineAngle(e.x + x, e.y + y, ang, e.fout() * 4 + 1f);
            });

        });

        hitMeltdown = new Effect(12, e -> {
            Draw.color(Pal.meltdownHit);
            Lines.stroke(e.fout() * 2f);

            Angles.randLenVectors(e.id, 6, e.finpow() * 18f, e.rotation, 360f, (x, y) -> {
                float ang = Mathf.angle(x, y);
                Lines.lineAngle(e.x + x, e.y + y, ang, e.fout() * 4 + 1f);
            });

        });

        hitLaser = new Effect(8, e -> {
            Draw.color(Color.white, Pal.heal, e.fin());
            Lines.stroke(0.5f + e.fout());
            Lines.circle(e.x, e.y, e.fin() * 5f);
        });

        despawn = new Effect(12, e -> {
            Draw.color(Pal.lighterOrange, Color.gray, e.fin());
            Lines.stroke(e.fout());

            Angles.randLenVectors(e.id, 7, e.fin() * 7f, e.rotation, 40f, (x, y) -> {
                float ang = Mathf.angle(x, y);
                Lines.lineAngle(e.x + x, e.y + y, ang, e.fout() * 2 + 1f);
            });

        });

        flakExplosion = new Effect(20, e -> {

            Draw.color(Pal.bulletYellow);
            e.scaled(6, i -> {
                Lines.stroke(3f * i.fout());
                Lines.circle(e.x, e.y, 3f + i.fin() * 10f);
            });

            Draw.color(Color.gray);

            Angles.randLenVectors(e.id, 5, 2f + 23f * e.finpow(), (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 3f + 0.5f);
            });

            Draw.color(Pal.lighterOrange);
            Lines.stroke(1f * e.fout());

            Angles.randLenVectors(e.id + 1, 4, 1f + 23f * e.finpow(), (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
            });

        });

        plasticExplosion = new Effect(24, e -> {

            Draw.color(Pal.plastaniumFront);
            e.scaled(7, i -> {
                Lines.stroke(3f * i.fout());
                Lines.circle(e.x, e.y, 3f + i.fin() * 24f);
            });

            Draw.color(Color.gray);

            Angles.randLenVectors(e.id, 7, 2f + 28f * e.finpow(), (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 4f + 0.5f);
            });

            Draw.color(Pal.plastaniumBack);
            Lines.stroke(1f * e.fout());

            Angles.randLenVectors(e.id + 1, 4, 1f + 25f * e.finpow(), (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
            });

        });

        plasticExplosionFlak = new Effect(28, e -> {

            Draw.color(Pal.plastaniumFront);
            e.scaled(7, i -> {
                Lines.stroke(3f * i.fout());
                Lines.circle(e.x, e.y, 3f + i.fin() * 34f);
            });

            Draw.color(Color.gray);

            Angles.randLenVectors(e.id, 7, 2f + 30f * e.finpow(), (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 4f + 0.5f);
            });

            Draw.color(Pal.plastaniumBack);
            Lines.stroke(1f * e.fout());

            Angles.randLenVectors(e.id + 1, 4, 1f + 30f * e.finpow(), (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
            });

        });

        blastExplosion = new Effect(22, e -> {

            Draw.color(Pal.missileYellow);
            e.scaled(6, i -> {
                Lines.stroke(3f * i.fout());
                Lines.circle(e.x, e.y, 3f + i.fin() * 15f);
            });

            Draw.color(Color.gray);

            Angles.randLenVectors(e.id, 5, 2f + 23f * e.finpow(), (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 4f + 0.5f);
            });

            Draw.color(Pal.missileYellowBack);
            Lines.stroke(1f * e.fout());

            Angles.randLenVectors(e.id + 1, 4, 1f + 23f * e.finpow(), (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
            });

        });

        artilleryTrail = new Effect(50, e -> {
            Draw.color(e.color);
            Fill.circle(e.x, e.y, e.rotation * e.fout());
        });

        incendTrail = new Effect(50, e -> {
            Draw.color(Pal.lightOrange);
            Fill.circle(e.x, e.y, e.rotation * e.fout());
        });

        missileTrail = new Effect(50, e -> {
            Draw.color(e.color);
            Fill.circle(e.x, e.y, e.rotation * e.fout());
        });

        absorb = new Effect(12, e -> {
            Draw.color(Pal.accent);
            Lines.stroke(2f * e.fout());
            Lines.circle(e.x, e.y, 5f * e.fout());
        });

        flakExplosionBig = new Effect(30, e -> {

            Draw.color(Pal.bulletYellowBack);
            e.scaled(6, i -> {
                Lines.stroke(3f * i.fout());
                Lines.circle(e.x, e.y, 3f + i.fin() * 25f);
            });

            Draw.color(Color.gray);

            Angles.randLenVectors(e.id, 6, 2f + 23f * e.finpow(), (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 4f + 0.5f);
            });

            Draw.color(Pal.bulletYellow);
            Lines.stroke(1f * e.fout());

            Angles.randLenVectors(e.id + 1, 4, 1f + 23f * e.finpow(), (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
            });

        });


        burning = new Effect(35f, e -> {
            Draw.color(Pal.lightFlame, Pal.darkFlame, e.fin());

            Angles.randLenVectors(e.id, 3, 2f + e.fin() * 7f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, 0.1f + e.fout() * 1.4f);
            });

        });

        fire = new Effect(50f, e -> {
            Draw.color(Pal.lightFlame, Pal.darkFlame, e.fin());

            Angles.randLenVectors(e.id, 2, 2f + e.fin() * 9f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, 0.2f + e.fslope() * 1.5f);
            });

            Draw.color();

            renderer.lights.add(e.x, e.y, 20f * e.fslope(), Pal.lightFlame, 0.5f);
        });

        fireSmoke = new Effect(35f, e -> {
            Draw.color(Color.gray);

            Angles.randLenVectors(e.id, 1, 2f + e.fin() * 7f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, 0.2f + e.fslope() * 1.5f);
            });

        });

        steam = new Effect(35f, e -> {
            Draw.color(Color.lightGray);

            Angles.randLenVectors(e.id, 2, 2f + e.fin() * 7f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, 0.2f + e.fslope() * 1.5f);
            });

        });

        fireballsmoke = new Effect(25f, e -> {
            Draw.color(Color.gray);

            Angles.randLenVectors(e.id, 1, 2f + e.fin() * 7f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, 0.2f + e.fout() * 1.5f);
            });

        });

        ballfire = new Effect(25f, e -> {
            Draw.color(Pal.lightFlame, Pal.darkFlame, e.fin());

            Angles.randLenVectors(e.id, 2, 2f + e.fin() * 7f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, 0.2f + e.fout() * 1.5f);
            });

        });

        freezing = new Effect(40f, e -> {
            Draw.color(Liquids.cryofluid.color);

            Angles.randLenVectors(e.id, 2, 1f + e.fin() * 2f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 1.2f);
            });

        });

        melting = new Effect(40f, e -> {
            Draw.color(Liquids.slag.color, Color.white, e.fout() / 5f + Mathf.randomSeedRange(e.id, 0.12f));

            Angles.randLenVectors(e.id, 2, 1f + e.fin() * 3f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, .2f + e.fout() * 1.2f);
            });

        });

        wet = new Effect(40f, e -> {
            Draw.color(Liquids.water.color);

            Angles.randLenVectors(e.id, 2, 1f + e.fin() * 2f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 1f);
            });

        });

        oily = new Effect(42f, e -> {
            Draw.color(Liquids.oil.color);

            Angles.randLenVectors(e.id, 2, 1f + e.fin() * 2f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 1f);
            });

        });

        overdriven = new Effect(20f, e -> {
            Draw.color(Pal.accent);

            Angles.randLenVectors(e.id, 2, 1f + e.fin() * 2f, (x, y) -> {
                Fill.square(e.x + x, e.y + y, e.fout() * 2.3f + 0.5f);
            });

        });

        dropItem = new Effect(20f, e -> {
            float length = 20f * e.finpow();
            float size = 7f * e.fout();

            Draw.rect(((Item)e.data).icon(Cicon.medium), e.x + Angles.trnsx(e.rotation, length), e.y + Angles.trnsy(e.rotation, length), size, size);
        });


        shockwave = new Effect(10f, 80f, e -> {
            Draw.color(Color.white, Color.lightGray, e.fin());
            Lines.stroke(e.fout() * 2f + 0.2f);
            Lines.circle(e.x, e.y, e.fin() * 28f);
        });

        bigShockwave = new Effect(10f, 80f, e -> {
            Draw.color(Color.white, Color.lightGray, e.fin());
            Lines.stroke(e.fout() * 3f);
            Lines.circle(e.x, e.y, e.fin() * 50f);
        });

        nuclearShockwave = new Effect(10f, 200f, e -> {
            Draw.color(Color.white, Color.lightGray, e.fin());
            Lines.stroke(e.fout() * 3f + 0.2f);
            Lines.circle(e.x, e.y, e.fin() * 140f);
        });

        impactShockwave = new Effect(13f, 300f, e -> {
            Draw.color(Pal.lighterOrange, Color.lightGray, e.fin());
            Lines.stroke(e.fout() * 4f + 0.2f);
            Lines.circle(e.x, e.y, e.fin() * 200f);
        });

        spawnShockwave = new Effect(20f, 400f, e -> {
            Draw.color(Color.white, Color.lightGray, e.fin());
            Lines.stroke(e.fout() * 3f + 0.5f);
            Lines.circle(e.x, e.y, e.fin() * (e.rotation + 50f));
        });

        explosion = new Effect(30, e -> {
            e.scaled(7, i -> {
                Lines.stroke(3f * i.fout());
                Lines.circle(e.x, e.y, 3f + i.fin() * 10f);
            });

            Draw.color(Color.gray);

            Angles.randLenVectors(e.id, 6, 2f + 19f * e.finpow(), (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 3f + 0.5f);
                Fill.circle(e.x + x / 2f, e.y + y / 2f, e.fout() * 1f);
            });

            Draw.color(Pal.lighterOrange, Pal.lightOrange, Color.gray, e.fin());
            Lines.stroke(1.5f * e.fout());

            Angles.randLenVectors(e.id + 1, 8, 1f + 23f * e.finpow(), (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
            });

        });

        dynamicExplosion = new Effect(30, e -> {
            float intensity = e.rotation;

            e.scaled(5 + intensity * 2, i -> {
                Lines.stroke(3.1f * i.fout());
                Lines.circle(e.x, e.y, (3f + i.fin() * 14f) * intensity);
            });

            Draw.color(Color.gray);

            Angles.randLenVectors(e.id, e.finpow(), (int)(6 * intensity), 21f * intensity, (x, y, in, out) -> {
                Fill.circle(e.x + x, e.y + y, out * (2f + intensity) * 3 + 0.5f);
                Fill.circle(e.x + x / 2f, e.y + y / 2f, out * (intensity) * 3);
            });

            Draw.color(Pal.lighterOrange, Pal.lightOrange, Color.gray, e.fin());
            Lines.stroke((1.7f * e.fout()) * (1f + (intensity - 1f) / 2f));

            Angles.randLenVectors(e.id + 1, e.finpow(), (int)(9 * intensity), 40f * intensity, (x, y, in, out) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + out * 4 * (3f + intensity));
            });

        });

        blockExplosion = new Effect(30, e -> {
            e.scaled(7, i -> {
                Lines.stroke(3.1f * i.fout());
                Lines.circle(e.x, e.y, 3f + i.fin() * 14f);
            });

            Draw.color(Color.gray);

            Angles.randLenVectors(e.id, 6, 2f + 19f * e.finpow(), (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 3f + 0.5f);
                Fill.circle(e.x + x / 2f, e.y + y / 2f, e.fout() * 1f);
            });

            Draw.color(Pal.lighterOrange, Pal.lightOrange, Color.gray, e.fin());
            Lines.stroke(1.7f * e.fout());

            Angles.randLenVectors(e.id + 1, 9, 1f + 23f * e.finpow(), (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
            });

        });

        blockExplosionSmoke = new Effect(30, e -> {
            Draw.color(Color.gray);

            Angles.randLenVectors(e.id, 6, 4f + 30f * e.finpow(), (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 3f);
                Fill.circle(e.x + x / 2f, e.y + y / 2f, e.fout() * 1f);
            });

        });


        shootSmall = new Effect(8, e -> {
            Draw.color(Pal.lighterOrange, Pal.lightOrange, e.fin());
            float w = 1f + 5 * e.fout();
            Drawf.tri(e.x, e.y, w, 15f * e.fout(), e.rotation);
            Drawf.tri(e.x, e.y, w, 3f * e.fout(), e.rotation + 180f);
        });

        shootHeal = new Effect(8, e -> {
            Draw.color(Pal.heal);
            float w = 1f + 5 * e.fout();
            Drawf.tri(e.x, e.y, w, 17f * e.fout(), e.rotation);
            Drawf.tri(e.x, e.y, w, 4f * e.fout(), e.rotation + 180f);
        });

        shootSmallSmoke = new Effect(20f, e -> {
            Draw.color(Pal.lighterOrange, Color.lightGray, Color.gray, e.fin());

            Angles.randLenVectors(e.id, 5, e.finpow() * 6f, e.rotation, 20f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 1.5f);
            });

        });

        shootBig = new Effect(9, e -> {
            Draw.color(Pal.lighterOrange, Pal.lightOrange, e.fin());
            float w = 1.2f + 7 * e.fout();
            Drawf.tri(e.x, e.y, w, 25f * e.fout(), e.rotation);
            Drawf.tri(e.x, e.y, w, 4f * e.fout(), e.rotation + 180f);
        });

        shootBig2 = new Effect(10, e -> {
            Draw.color(Pal.lightOrange, Color.gray, e.fin());
            float w = 1.2f + 8 * e.fout();
            Drawf.tri(e.x, e.y, w, 29f * e.fout(), e.rotation);
            Drawf.tri(e.x, e.y, w, 5f * e.fout(), e.rotation + 180f);
        });

        shootBigSmoke = new Effect(17f, e -> {
            Draw.color(Pal.lighterOrange, Color.lightGray, Color.gray, e.fin());

            Angles.randLenVectors(e.id, 8, e.finpow() * 19f, e.rotation, 10f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 2f + 0.2f);
            });

        });

        shootBigSmoke2 = new Effect(18f, e -> {
            Draw.color(Pal.lightOrange, Color.lightGray, Color.gray, e.fin());

            Angles.randLenVectors(e.id, 9, e.finpow() * 23f, e.rotation, 20f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, e.fout() * 2.4f + 0.2f);
            });

        });

        shootSmallFlame = new Effect(32f, e -> {
            Draw.color(Pal.lightFlame, Pal.darkFlame, Color.gray, e.fin());

            Angles.randLenVectors(e.id, 8, e.finpow() * 60f, e.rotation, 10f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, 0.65f + e.fout() * 1.5f);
            });

        });

        shootPyraFlame = new Effect(33f, e -> {
            Draw.color(Pal.lightPyraFlame, Pal.darkPyraFlame, Color.gray, e.fin());

            Angles.randLenVectors(e.id, 10, e.finpow() * 70f, e.rotation, 10f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, 0.65f + e.fout() * 1.6f);
            });

        });

        shootLiquid = new Effect(40f, e -> {
            Draw.color(e.color, Color.white, e.fout() / 6f + Mathf.randomSeedRange(e.id, 0.1f));

            Angles.randLenVectors(e.id, 6, e.finpow() * 60f, e.rotation, 11f, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, 0.5f + e.fout() * 2.5f);
            });

        });

        shellEjectSmall = new GroundEffect(30f, 400f, e -> {
            Draw.color(Pal.lightOrange, Color.lightGray, Pal.lightishGray, e.fin());
            float rot = Math.abs(e.rotation) + 90f;

            int i = Mathf.sign(e.rotation);

            float len = (2f + e.finpow() * 6f) * i;
            float lr = rot + e.fin() * 30f * i;
            Fill.rect(e.x + Angles.trnsx(lr, len) + Mathf.randomSeedRange(e.id + i + 7, 3f * e.fin()),
            e.y + Angles.trnsy(lr, len) + Mathf.randomSeedRange(e.id + i + 8, 3f * e.fin()),
            1f, 2f, rot + e.fin() * 50f * i);

        });

        shellEjectMedium = new GroundEffect(34f, 400f, e -> {
            Draw.color(Pal.lightOrange, Color.lightGray, Pal.lightishGray, e.fin());
            float rot = e.rotation + 90f;
            for(int i : Mathf.signs){
                float len = (2f + e.finpow() * 10f) * i;
                float lr = rot + e.fin() * 20f * i;
                Draw.rect(Core.atlas.find("casing"),
                e.x + Angles.trnsx(lr, len) + Mathf.randomSeedRange(e.id + i + 7, 3f * e.fin()),
                e.y + Angles.trnsy(lr, len) + Mathf.randomSeedRange(e.id + i + 8, 3f * e.fin()),
                2f, 3f, rot);
            }

            Draw.color(Color.lightGray, Color.gray, e.fin());

            for(int i : Mathf.signs){
                float ex = e.x, ey = e.y, fout = e.fout();
                Angles.randLenVectors(e.id, 4, 1f + e.finpow() * 11f, e.rotation + 90f * i, 20f, (x, y) -> {
                    Fill.circle(ex + x, ey + y, fout * 1.5f);
                });
            }

        });

        shellEjectBig = new GroundEffect(22f, 400f, e -> {
            Draw.color(Pal.lightOrange, Color.lightGray, Pal.lightishGray, e.fin());
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

            Draw.color(Color.lightGray);

            for(int i : Mathf.signs){
                float ex = e.x, ey = e.y, fout = e.fout();
                Angles.randLenVectors(e.id, 4, -e.finpow() * 15f, e.rotation + 90f * i, 25f, (x, y) -> {
                    Fill.circle(ex + x, ey + y, fout * 2f);
                });
            }

        });

        lancerLaserShoot = new Effect(21f, e -> {
            Draw.color(Pal.lancerLaser);

            for(int i : Mathf.signs){
                Drawf.tri(e.x, e.y, 4f * e.fout(), 29f, e.rotation + 90f * i);
            }

        });

        lancerLaserShootSmoke = new Effect(26f, e -> {
            Draw.color(Pal.lancerLaser);

            Angles.randLenVectors(e.id, 7, 80f, e.rotation, 0f, (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fout() * 9f);
            });

        });

        lancerLaserCharge = new Effect(38f, e -> {
            Draw.color(Pal.lancerLaser);

            Angles.randLenVectors(e.id, 2, 1f + 20f * e.fout(), e.rotation, 120f, (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fslope() * 3f + 1f);
            });

        });

        lancerLaserChargeBegin = new Effect(71f, e -> {
            Draw.color(Pal.lancerLaser);
            Fill.circle(e.x, e.y, e.fin() * 3f);

            Draw.color();
            Fill.circle(e.x, e.y, e.fin() * 2f);
        });

        lightningCharge = new Effect(38f, e -> {
            Draw.color(Pal.lancerLaser);

            Angles.randLenVectors(e.id, 2, 1f + 20f * e.fout(), e.rotation, 120f, (x, y) -> {
                Drawf.tri(e.x + x, e.y + y, e.fslope() * 3f + 1, e.fslope() * 3f + 1, Mathf.angle(x, y));
            });

        });

        lightningShoot = new Effect(12f, e -> {
            Draw.color(Color.white, Pal.lancerLaser, e.fin());
            Lines.stroke(e.fout() * 1.2f + 0.5f);

            Angles.randLenVectors(e.id, 7, 25f * e.finpow(), e.rotation, 50f, (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fin() * 5f + 2f);
            });

        });


        reactorsmoke = new Effect(17, e -> {
            Angles.randLenVectors(e.id, 4, e.fin() * 8f, (x, y) -> {
                float size = 1f + e.fout() * 5f;
                Draw.color(Color.lightGray, Color.gray, e.fin());
                Draw.rect("circle", e.x + x, e.y + y, size, size);
            });
        });
        nuclearsmoke = new Effect(40, e -> {
            Angles.randLenVectors(e.id, 4, e.fin() * 13f, (x, y) -> {
                float size = e.fslope() * 4f;
                Draw.color(Color.lightGray, Color.gray, e.fin());
                Draw.rect("circle", e.x + x, e.y + y, size, size);
            });
        });
        nuclearcloud = new Effect(90, 200f, e -> {
            Angles.randLenVectors(e.id, 10, e.finpow() * 90f, (x, y) -> {
                float size = e.fout() * 14f;
                Draw.color(Color.lime, Color.gray, e.fin());
                Draw.rect("circle", e.x + x, e.y + y, size, size);
            });
        });
        impactsmoke = new Effect(60, e -> {
            Angles.randLenVectors(e.id, 7, e.fin() * 20f, (x, y) -> {
                float size = e.fslope() * 4f;
                Draw.color(Color.lightGray, Color.gray, e.fin());
                Draw.rect("circle", e.x + x, e.y + y, size, size);
            });
        });
        impactcloud = new Effect(140, 400f, e -> {
            Angles.randLenVectors(e.id, 20, e.finpow() * 160f, (x, y) -> {
                float size = e.fout() * 15f;
                Draw.color(Pal.lighterOrange, Color.lightGray, e.fin());
                Draw.rect("circle", e.x + x, e.y + y, size, size);
            });
        });
        redgeneratespark = new Effect(18, e -> {
            Angles.randLenVectors(e.id, 5, e.fin() * 8f, (x, y) -> {
                float len = e.fout() * 4f;
                Draw.color(Pal.redSpark, Color.gray, e.fin());
                Draw.rect("circle", e.x + x, e.y + y, len, len);
            });
        });
        generatespark = new Effect(18, e -> {
            Angles.randLenVectors(e.id, 5, e.fin() * 8f, (x, y) -> {
                float len = e.fout() * 4f;
                Draw.color(Pal.orangeSpark, Color.gray, e.fin());
                Draw.rect("circle", e.x + x, e.y + y, len, len);
            });
        });
        fuelburn = new Effect(23, e -> {
            Angles.randLenVectors(e.id, 5, e.fin() * 9f, (x, y) -> {
                float len = e.fout() * 4f;
                Draw.color(Color.lightGray, Color.gray, e.fin());
                Draw.rect("circle", e.x + x, e.y + y, len, len);
            });
        });
        plasticburn = new Effect(40, e -> {
            Angles.randLenVectors(e.id, 5, 3f + e.fin() * 5f, (x, y) -> {
                Draw.color(Color.valueOf("e9ead3"), Color.gray, e.fin());
                Fill.circle(e.x + x, e.y + y, e.fout() * 1f);
            });
        });
        pulverize = new Effect(40, e -> {
            Angles.randLenVectors(e.id, 5, 3f + e.fin() * 8f, (x, y) -> {
                Draw.color(Pal.stoneGray);
                Fill.square(e.x + x, e.y + y, e.fout() * 2f + 0.5f, 45);
            });
        });
        pulverizeRed = new Effect(40, e -> {
            Angles.randLenVectors(e.id, 5, 3f + e.fin() * 8f, (x, y) -> {
                Draw.color(Pal.redDust, Pal.stoneGray, e.fin());
                Fill.square(e.x + x, e.y + y, e.fout() * 2f + 0.5f, 45);
            });
        });
        pulverizeRedder = new Effect(40, e -> {
            Angles.randLenVectors(e.id, 5, 3f + e.fin() * 9f, (x, y) -> {
                Draw.color(Pal.redderDust, Pal.stoneGray, e.fin());
                Fill.square(e.x + x, e.y + y, e.fout() * 2.5f + 0.5f, 45);
            });
        });
        pulverizeSmall = new Effect(30, e -> {
            Angles.randLenVectors(e.id, 3, e.fin() * 5f, (x, y) -> {
                Draw.color(Pal.stoneGray);
                Fill.square(e.x + x, e.y + y, e.fout() * 1f + 0.5f, 45);
            });
        });
        pulverizeMedium = new Effect(30, e -> {
            Angles.randLenVectors(e.id, 5, 3f + e.fin() * 8f, (x, y) -> {
                Draw.color(Pal.stoneGray);
                Fill.square(e.x + x, e.y + y, e.fout() * 1f + 0.5f, 45);
            });
        });
        producesmoke = new Effect(12, e -> {
            Angles.randLenVectors(e.id, 8, 4f + e.fin() * 18f, (x, y) -> {
                Draw.color(Color.white, Pal.accent, e.fin());
                Fill.square(e.x + x, e.y + y, 1f + e.fout() * 3f, 45);
            });
        });
        smeltsmoke = new Effect(15, e -> {
            Angles.randLenVectors(e.id, 6, 4f + e.fin() * 5f, (x, y) -> {
                Draw.color(Color.white, e.color, e.fin());
                Fill.square(e.x + x, e.y + y, 0.5f + e.fout() * 2f, 45);
            });
        });
        formsmoke = new Effect(40, e -> {
            Angles.randLenVectors(e.id, 6, 5f + e.fin() * 8f, (x, y) -> {
                Draw.color(Pal.plasticSmoke, Color.lightGray, e.fin());
                Fill.square(e.x + x, e.y + y, 0.2f + e.fout() * 2f, 45);
            });
        });
        blastsmoke = new Effect(26, e -> {
            Angles.randLenVectors(e.id, 12, 1f + e.fin() * 23f, (x, y) -> {
                float size = 2f + e.fout() * 6f;
                Draw.color(Color.lightGray, Color.darkGray, e.fin());
                Draw.rect("circle", e.x + x, e.y + y, size, size);
            });
        });
        lava = new Effect(18, e -> {
            Angles.randLenVectors(e.id, 3, 1f + e.fin() * 10f, (x, y) -> {
                float size = e.fslope() * 4f;
                Draw.color(Color.orange, Color.gray, e.fin());
                Draw.rect("circle", e.x + x, e.y + y, size, size);
            });
        });
        dooropen = new Effect(10, e -> {
            Lines.stroke(e.fout() * 1.6f);
            Lines.square(e.x, e.y, tilesize / 2f + e.fin() * 2f);
        });
        doorclose = new Effect(10, e -> {
            Lines.stroke(e.fout() * 1.6f);
            Lines.square(e.x, e.y, tilesize / 2f + e.fout() * 2f);
        });
        dooropenlarge = new Effect(10, e -> {
            Lines.stroke(e.fout() * 1.6f);
            Lines.square(e.x, e.y, tilesize + e.fin() * 2f);
        });
        doorcloselarge = new Effect(10, e -> {
            Lines.stroke(e.fout() * 1.6f);
            Lines.square(e.x, e.y, tilesize + e.fout() * 2f);
        });
        purify = new Effect(10, e -> {
            Draw.color(Color.royal, Color.gray, e.fin());
            Lines.stroke(2f);
            Lines.spikes(e.x, e.y, e.fin() * 4f, 2, 6);
        });
        purifyoil = new Effect(10, e -> {
            Draw.color(Color.black, Color.gray, e.fin());
            Lines.stroke(2f);
            Lines.spikes(e.x, e.y, e.fin() * 4f, 2, 6);
        });
        purifystone = new Effect(10, e -> {
            Draw.color(Color.orange, Color.gray, e.fin());
            Lines.stroke(2f);
            Lines.spikes(e.x, e.y, e.fin() * 4f, 2, 6);
        });
        generate = new Effect(11, e -> {
            Draw.color(Color.orange, Color.yellow, e.fin());
            Lines.stroke(1f);
            Lines.spikes(e.x, e.y, e.fin() * 5f, 2, 8);
        });
        mine = new Effect(20, e -> {
            Angles.randLenVectors(e.id, 6, 3f + e.fin() * 6f, (x, y) -> {
                Draw.color(e.color, Color.lightGray, e.fin());
                Fill.square(e.x + x, e.y + y, e.fout() * 2f, 45);
            });
        });
        mineBig = new Effect(30, e -> {
            Angles.randLenVectors(e.id, 6, 4f + e.fin() * 8f, (x, y) -> {
                Draw.color(e.color, Color.lightGray, e.fin());
                Fill.square(e.x + x, e.y + y, e.fout() * 2f + 0.2f, 45);
            });
        });
        mineHuge = new Effect(40, e -> {
            Angles.randLenVectors(e.id, 8, 5f + e.fin() * 10f, (x, y) -> {
                Draw.color(e.color, Color.lightGray, e.fin());
                Fill.square(e.x + x, e.y + y, e.fout() * 2f + 0.5f, 45);
            });
        });
        smelt = new Effect(20, e -> {
            Angles.randLenVectors(e.id, 6, 2f + e.fin() * 5f, (x, y) -> {
                Draw.color(Color.white, e.color, e.fin());
                Fill.square(e.x + x, e.y + y, 0.5f + e.fout() * 2f, 45);
            });
        });
        teleportActivate = new Effect(50, e -> {
            Draw.color(e.color);

            e.scaled(8f, e2 -> {
                Lines.stroke(e2.fout() * 4f);
                Lines.circle(e2.x, e2.y, 4f + e2.fin() * 27f);
            });

            Lines.stroke(e.fout() * 2f);

            Angles.randLenVectors(e.id, 30, 4f + 40f * e.fin(), (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fin() * 4f + 1f);
            });

        });
        teleport = new Effect(60, e -> {
            Draw.color(e.color);
            Lines.stroke(e.fin() * 2f);
            Lines.circle(e.x, e.y, 7f + e.fout() * 8f);

            Angles.randLenVectors(e.id, 20, 6f + 20f * e.fout(), (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fin() * 4f + 1f);
            });

        });
        teleportOut = new Effect(20, e -> {
            Draw.color(e.color);
            Lines.stroke(e.fout() * 2f);
            Lines.circle(e.x, e.y, 7f + e.fin() * 8f);

            Angles.randLenVectors(e.id, 20, 4f + 20f * e.fin(), (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fslope() * 4f + 1f);
            });

        });
        ripple = new GroundEffect(false, 30, e -> {
            Draw.color(Tmp.c1.set(e.color).mul(1.2f));
            Lines.stroke(e.fout() + 0.4f);
            Lines.circle(e.x, e.y, 2f + e.fin() * 4f);
        });

        bubble = new Effect(20, e -> {
            Draw.color(Tmp.c1.set(e.color).shiftValue(0.1f));
            Lines.stroke(e.fout() + 0.2f);
            Angles.randLenVectors(e.id, 2, 8f, (x, y) -> {
                Lines.circle(e.x + x, e.y + y, 1f + e.fin() * 3f);
            });
        });

        launch = new Effect(28, e -> {
            Draw.color(Pal.command);
            Lines.stroke(e.fout() * 2f);
            Lines.circle(e.x, e.y, 4f + e.finpow() * 120f);
        });

        healWaveMend = new Effect(40, e -> {
            Draw.color(e.color);
            Lines.stroke(e.fout() * 2f);
            Lines.circle(e.x, e.y, e.finpow() * e.rotation);
        });

        overdriveWave = new Effect(50, e -> {
            Draw.color(e.color);
            Lines.stroke(e.fout() * 1f);
            Lines.circle(e.x, e.y, e.finpow() * e.rotation);
        });

        healBlock = new Effect(20, e -> {
            Draw.color(Pal.heal);
            Lines.stroke(2f * e.fout() + 0.5f);
            Lines.square(e.x, e.y, 1f + (e.fin() * e.rotation * tilesize / 2f - 1f));
        });

        healBlockFull = new Effect(20, e -> {
            Draw.color(e.color);
            Draw.alpha(e.fout());
            Fill.square(e.x, e.y, e.rotation * tilesize / 2f);
        });

        overdriveBlockFull = new Effect(60, e -> {
            Draw.color(e.color);
            Draw.alpha(e.fslope() * 0.4f);
            Fill.square(e.x, e.y, e.rotation * tilesize);
        });

        shieldBreak = new Effect(40, e -> {
            Draw.color(Pal.accent);
            Lines.stroke(3f * e.fout());
            Lines.poly(e.x, e.y, 6, e.rotation + e.fin(), 90);
        });

        coreLand = new Effect(120f, e -> {
        });
    }
}
