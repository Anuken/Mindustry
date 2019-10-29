package io.anuke.mindustry.content;

import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.ctype.ContentList;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.bullet.*;
import io.anuke.mindustry.entities.effect.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.world.*;

import static io.anuke.mindustry.Vars.*;

public class Bullets implements ContentList{
    public static BulletType

    //artillery
    artilleryDense, arilleryPlastic, artilleryPlasticFrag, artilleryHoming, artlleryIncendiary, artilleryExplosive, artilleryUnit,

    //flak
    flakScrap, flakLead, flakPlastic, flakExplosive, flakSurge, flakGlass, glassFrag,

    //missiles
    missileExplosive, missileIncendiary, missileSurge, missileJavelin, missileSwarm, missileRevenant,

    //standard
    standardCopper, standardDense, standardThorium, standardHoming, standardIncendiary, standardMechSmall,
    standardGlaive, standardDenseBig, standardThoriumBig, standardIncendiaryBig,

    //electric
    lancerLaser, meltdownLaser, lightning, arc, damageLightning,

    //liquid
    waterShot, cryoShot, slagShot, oilShot,

    //environment, misc.
    fireball, basicFlame, pyraFlame, driverBolt, healBullet, healBulletBig, frag, eruptorShot,

    //bombs
    bombExplosive, bombIncendiary, bombOil;

    @Override
    public void load(){

        artilleryDense = new ArtilleryBulletType(3f, 0, "shell"){{
            hitEffect = Fx.flakExplosion;
            knockback = 0.8f;
            lifetime = 50f;
            bulletWidth = bulletHeight = 11f;
            collidesTiles = false;
            splashDamageRadius = 25f;
            splashDamage = 33f;
        }};

        artilleryPlasticFrag = new BasicBulletType(2.5f, 10, "bullet"){{
            bulletWidth = 10f;
            bulletHeight = 12f;
            bulletShrink = 1f;
            lifetime = 15f;
            backColor = Pal.plastaniumBack;
            frontColor = Pal.plastaniumFront;
            despawnEffect = Fx.none;
        }};

        arilleryPlastic = new ArtilleryBulletType(3.4f, 0, "shell"){{
            hitEffect = Fx.plasticExplosion;
            knockback = 1f;
            lifetime = 55f;
            bulletWidth = bulletHeight = 13f;
            collidesTiles = false;
            splashDamageRadius = 35f;
            splashDamage = 45f;
            fragBullet = artilleryPlasticFrag;
            fragBullets = 10;
            backColor = Pal.plastaniumBack;
            frontColor = Pal.plastaniumFront;
        }};

        artilleryHoming = new ArtilleryBulletType(3f, 0, "shell"){{
            hitEffect = Fx.flakExplosion;
            knockback = 0.8f;
            lifetime = 45f;
            bulletWidth = bulletHeight = 11f;
            collidesTiles = false;
            splashDamageRadius = 25f;
            splashDamage = 33f;
            homingPower = 2f;
            homingRange = 50f;
        }};

        artlleryIncendiary = new ArtilleryBulletType(3f, 0, "shell"){{
            hitEffect = Fx.blastExplosion;
            knockback = 0.8f;
            lifetime = 60f;
            bulletWidth = bulletHeight = 13f;
            collidesTiles = false;
            splashDamageRadius = 25f;
            splashDamage = 30f;
            status = StatusEffects.burning;
            frontColor = Pal.lightishOrange;
            backColor = Pal.lightOrange;
            trailEffect = Fx.incendTrail;
        }};

        artilleryExplosive = new ArtilleryBulletType(2f, 0, "shell"){{
            hitEffect = Fx.blastExplosion;
            knockback = 0.8f;
            lifetime = 70f;
            bulletWidth = bulletHeight = 14f;
            collidesTiles = false;
            ammoMultiplier = 4f;
            splashDamageRadius = 45f;
            splashDamage = 50f;
            backColor = Pal.missileYellowBack;
            frontColor = Pal.missileYellow;
        }};

        artilleryUnit = new ArtilleryBulletType(2f, 0, "shell"){{
            hitEffect = Fx.blastExplosion;
            knockback = 0.8f;
            lifetime = 90f;
            bulletWidth = bulletHeight = 14f;
            collides = true;
            collidesTiles = true;
            splashDamageRadius = 20f;
            splashDamage = 38f;
            backColor = Pal.bulletYellowBack;
            frontColor = Pal.bulletYellow;
        }};

        glassFrag = new BasicBulletType(3f, 6, "bullet"){{
            bulletWidth = 5f;
            bulletHeight = 12f;
            bulletShrink = 1f;
            lifetime = 20f;
            backColor = Pal.gray;
            frontColor = Color.white;
            despawnEffect = Fx.none;
        }};

        flakLead = new FlakBulletType(4.2f, 3){{
            lifetime = 60f;
            ammoMultiplier = 4f;
            shootEffect = Fx.shootSmall;
            bulletWidth = 6f;
            bulletHeight = 8f;
            hitEffect = Fx.flakExplosion;
            splashDamage = 27f;
            splashDamageRadius = 15f;
        }};

        flakScrap = new FlakBulletType(4f, 3){{
            lifetime = 60f;
            ammoMultiplier = 5f;
            shootEffect = Fx.shootSmall;
            reloadMultiplier = 0.5f;
            bulletWidth = 6f;
            bulletHeight = 8f;
            hitEffect = Fx.flakExplosion;
            splashDamage = 22f;
            splashDamageRadius = 24f;
        }};

        flakGlass = new FlakBulletType(4f, 3){{
            lifetime = 70f;
            ammoMultiplier = 5f;
            shootEffect = Fx.shootSmall;
            reloadMultiplier = 0.8f;
            bulletWidth = 6f;
            bulletHeight = 8f;
            hitEffect = Fx.flakExplosion;
            splashDamage = 30f;
            splashDamageRadius = 26f;
            fragBullet = glassFrag;
            fragBullets = 6;
        }};

        flakPlastic = new FlakBulletType(4f, 6){{
            splashDamageRadius = 50f;
            splashDamage = 25f;
            fragBullet = artilleryPlasticFrag;
            fragBullets = 6;
            hitEffect = Fx.plasticExplosion;
            frontColor = Pal.plastaniumFront;
            backColor = Pal.plastaniumBack;
            shootEffect = Fx.shootBig;
        }};

        flakExplosive = new FlakBulletType(4f, 5){{
            //default bullet type, no changes
            shootEffect = Fx.shootBig;
            ammoMultiplier = 4f;
        }};

        flakSurge = new FlakBulletType(4f, 7){{
            splashDamage = 33f;
            lightining = 2;
            lightningLength = 12;
            shootEffect = Fx.shootBig;
        }};

        missileExplosive = new MissileBulletType(2.7f, 10, "missile"){{
            bulletWidth = 8f;
            bulletHeight = 8f;
            bulletShrink = 0f;
            drag = -0.01f;
            splashDamageRadius = 30f;
            splashDamage = 30f;
            ammoMultiplier = 4f;
            lifetime = 150f;
            hitEffect = Fx.blastExplosion;
            despawnEffect = Fx.blastExplosion;
        }};

        missileIncendiary = new MissileBulletType(2.9f, 12, "missile"){{
            frontColor = Pal.lightishOrange;
            backColor = Pal.lightOrange;
            bulletWidth = 7f;
            bulletHeight = 8f;
            bulletShrink = 0f;
            drag = -0.01f;
            homingPower = 7f;
            splashDamageRadius = 10f;
            splashDamage = 10f;
            lifetime = 160f;
            hitEffect = Fx.blastExplosion;
            status = StatusEffects.burning;
        }};

        missileSurge = new MissileBulletType(4.4f, 15, "bullet"){{
            bulletWidth = 8f;
            bulletHeight = 8f;
            bulletShrink = 0f;
            drag = -0.01f;
            splashDamageRadius = 30f;
            splashDamage = 22f;
            lifetime = 150f;
            hitEffect = Fx.blastExplosion;
            despawnEffect = Fx.blastExplosion;
            lightining = 2;
            lightningLength = 14;
        }};

        missileJavelin = new MissileBulletType(5f, 10.5f, "missile"){{
            bulletWidth = 8f;
            bulletHeight = 8f;
            bulletShrink = 0f;
            drag = -0.003f;
            keepVelocity = false;
            splashDamageRadius = 20f;
            splashDamage = 1f;
            lifetime = 90f;
            trailColor = Color.valueOf("b6c6fd");
            hitEffect = Fx.blastExplosion;
            despawnEffect = Fx.blastExplosion;
            backColor = Pal.bulletYellowBack;
            frontColor = Pal.bulletYellow;
            weaveScale = 8f;
            weaveMag = 2f;
        }};

        missileSwarm = new MissileBulletType(2.7f, 12, "missile"){{
            bulletWidth = 8f;
            bulletHeight = 8f;
            bulletShrink = 0f;
            drag = -0.003f;
            homingRange = 60f;
            keepVelocity = false;
            splashDamageRadius = 25f;
            splashDamage = 10f;
            lifetime = 120f;
            trailColor = Color.gray;
            backColor = Pal.bulletYellowBack;
            frontColor = Pal.bulletYellow;
            hitEffect = Fx.blastExplosion;
            despawnEffect = Fx.blastExplosion;
            weaveScale = 8f;
            weaveMag = 2f;
        }};

        missileRevenant = new MissileBulletType(2.7f, 12, "missile"){{
            bulletWidth = 8f;
            bulletHeight = 8f;
            bulletShrink = 0f;
            drag = -0.003f;
            homingRange = 60f;
            keepVelocity = false;
            splashDamageRadius = 25f;
            splashDamage = 10f;
            lifetime = 60f;
            trailColor = Pal.unitBack;
            backColor = Pal.unitBack;
            frontColor = Pal.unitFront;
            hitEffect = Fx.blastExplosion;
            despawnEffect = Fx.blastExplosion;
            weaveScale = 6f;
            weaveMag = 1f;
        }};

        standardCopper = new BasicBulletType(2.5f, 9, "bullet"){{
            bulletWidth = 7f;
            bulletHeight = 9f;
            lifetime = 60f;
            shootEffect = Fx.shootSmall;
            smokeEffect = Fx.shootSmallSmoke;
            ammoMultiplier = 2;
        }};

        standardDense = new BasicBulletType(3.5f, 18, "bullet"){{
            bulletWidth = 9f;
            bulletHeight = 12f;
            reloadMultiplier = 0.6f;
            ammoMultiplier = 4;
            lifetime = 60f;
        }};

        standardThorium = new BasicBulletType(4f, 29, "bullet"){{
            bulletWidth = 10f;
            bulletHeight = 13f;
            shootEffect = Fx.shootBig;
            smokeEffect = Fx.shootBigSmoke;
            ammoMultiplier = 4;
            lifetime = 60f;
        }};

        standardHoming = new BasicBulletType(3f, 9, "bullet"){{
            bulletWidth = 7f;
            bulletHeight = 9f;
            homingPower = 5f;
            reloadMultiplier = 1.4f;
            ammoMultiplier = 5;
            lifetime = 60f;
        }};

        standardIncendiary = new BasicBulletType(3.2f, 11, "bullet"){{
            bulletWidth = 10f;
            bulletHeight = 12f;
            frontColor = Pal.lightishOrange;
            backColor = Pal.lightOrange;
            status = StatusEffects.burning;
            inaccuracy = 3f;
            lifetime = 60f;
        }};

        standardGlaive = new BasicBulletType(4f, 7.5f, "bullet"){{
            bulletWidth = 10f;
            bulletHeight = 12f;
            frontColor = Color.valueOf("feb380");
            backColor = Color.valueOf("ea8878");
            status = StatusEffects.burning;
            lifetime = 60f;
        }};

        standardMechSmall = new BasicBulletType(4f, 9, "bullet"){{
            bulletWidth = 11f;
            bulletHeight = 14f;
            lifetime = 40f;
            inaccuracy = 5f;
            despawnEffect = Fx.hitBulletSmall;
        }};

        standardDenseBig = new BasicBulletType(7f, 42, "bullet"){{
            bulletWidth = 15f;
            bulletHeight = 21f;
            shootEffect = Fx.shootBig;
        }};

        standardThoriumBig = new BasicBulletType(8f, 65, "bullet"){{
            bulletWidth = 16f;
            bulletHeight = 23f;
            shootEffect = Fx.shootBig;
        }};

        standardIncendiaryBig = new BasicBulletType(7f, 38, "bullet"){{
            bulletWidth = 16f;
            bulletHeight = 21f;
            frontColor = Pal.lightishOrange;
            backColor = Pal.lightOrange;
            status = StatusEffects.burning;
            shootEffect = Fx.shootBig;
        }};

        damageLightning = new BulletType(0.0001f, 0f){{
            lifetime = Lightning.lifetime;
            hitEffect = Fx.hitLancer;
            despawnEffect = Fx.none;
            status = StatusEffects.shocked;
            statusDuration = 10f;
        }};

        healBullet = new HealBulletType(5.2f, 13){{
            healPercent = 3f;
        }};

        healBulletBig = new HealBulletType(5.2f, 15){{
            healPercent = 5.5f;
        }};

        fireball = new BulletType(1f, 4){
            {
                pierce = true;
                hitTiles = false;
                collides = false;
                collidesTiles = false;
                drag = 0.03f;
                hitEffect = despawnEffect = Fx.none;
            }

            @Override
            public void init(Bullet b){
                b.velocity().setLength(0.6f + Mathf.random(2f));
            }

            @Override
            public void draw(Bullet b){
                Draw.color(Pal.lightFlame, Pal.darkFlame, Color.gray, b.fin());
                Fill.circle(b.x, b.y, 3f * b.fout());
                Draw.reset();
            }

            @Override
            public void update(Bullet b){
                if(Mathf.chance(0.04 * Time.delta())){
                    Tile tile = world.tileWorld(b.x, b.y);
                    if(tile != null){
                        Fire.create(tile);
                    }
                }

                if(Mathf.chance(0.1 * Time.delta())){
                    Effects.effect(Fx.fireballsmoke, b.x, b.y);
                }

                if(Mathf.chance(0.1 * Time.delta())){
                    Effects.effect(Fx.ballfire, b.x, b.y);
                }
            }
        };

        basicFlame = new BulletType(3f, 6f){
            {
                ammoMultiplier = 3f;
                hitSize = 7f;
                lifetime = 42f;
                pierce = true;
                drag = 0.05f;
                statusDuration = 60f * 4;
                shootEffect = Fx.shootSmallFlame;
                hitEffect = Fx.hitFlameSmall;
                despawnEffect = Fx.none;
                status = StatusEffects.burning;
            }

            @Override
            public float range(){
                return 50f;
            }

            @Override
            public void draw(Bullet b){
            }
        };

        pyraFlame = new BulletType(3.3f, 9f){
            {
                ammoMultiplier = 4f;
                hitSize = 7f;
                lifetime = 42f;
                pierce = true;
                drag = 0.05f;
                statusDuration = 60f * 6;
                shootEffect = Fx.shootPyraFlame;
                hitEffect = Fx.hitFlameSmall;
                despawnEffect = Fx.none;
                status = StatusEffects.burning;
            }

            @Override
            public void draw(Bullet b){
            }
        };

        lancerLaser = new BulletType(0.001f, 140){
            Color[] colors = {Pal.lancerLaser.cpy().mul(1f, 1f, 1f, 0.4f), Pal.lancerLaser, Color.white};
            float[] tscales = {1f, 0.7f, 0.5f, 0.2f};
            float[] lenscales = {1f, 1.1f, 1.13f, 1.14f};
            float length = 160f;

            {
                hitEffect = Fx.hitLancer;
                despawnEffect = Fx.none;
                hitSize = 4;
                lifetime = 16f;
                pierce = true;
            }

            @Override
            public float range(){
                return length;
            }

            @Override
            public void init(Bullet b){
                Damage.collideLine(b, b.getTeam(), hitEffect, b.x, b.y, b.rot(), length);
            }

            @Override
            public void draw(Bullet b){
                float f = Mathf.curve(b.fin(), 0f, 0.2f);
                float baseLen = length * f;

                Lines.lineAngle(b.x, b.y, b.rot(), baseLen);
                for(int s = 0; s < 3; s++){
                    Draw.color(colors[s]);
                    for(int i = 0; i < tscales.length; i++){
                        Lines.stroke(7f * b.fout() * (s == 0 ? 1.5f : s == 1 ? 1f : 0.3f) * tscales[i]);
                        Lines.lineAngle(b.x, b.y, b.rot(), baseLen * lenscales[i]);
                    }
                }
                Draw.reset();
            }
        };

        meltdownLaser = new BulletType(0.001f, 70){
            Color tmpColor = new Color();
            Color[] colors = {Color.valueOf("ec745855"), Color.valueOf("ec7458aa"), Color.valueOf("ff9c5a"), Color.white};
            float[] tscales = {1f, 0.7f, 0.5f, 0.2f};
            float[] strokes = {2f, 1.5f, 1f, 0.3f};
            float[] lenscales = {1f, 1.12f, 1.15f, 1.17f};
            float length = 220f;

            {
                hitEffect = Fx.hitMeltdown;
                despawnEffect = Fx.none;
                hitSize = 4;
                drawSize = 420f;
                lifetime = 16f;
                pierce = true;
            }

            @Override
            public void update(Bullet b){
                if(b.timer.get(1, 5f)){
                    Damage.collideLine(b, b.getTeam(), hitEffect, b.x, b.y, b.rot(), length, true);
                }
                Effects.shake(1f, 1f, b.x, b.y);
            }

            @Override
            public void hit(Bullet b, float hitx, float hity){
                Effects.effect(hitEffect, colors[2], hitx, hity);
                if(Mathf.chance(0.4)){
                    Fire.create(world.tileWorld(hitx + Mathf.range(5f), hity + Mathf.range(5f)));
                }
            }

            @Override
            public void draw(Bullet b){
                float baseLen = (length) * b.fout();

                Lines.lineAngle(b.x, b.y, b.rot(), baseLen);
                for(int s = 0; s < colors.length; s++){
                    Draw.color(tmpColor.set(colors[s]).mul(1f + Mathf.absin(Time.time(), 1f, 0.1f)));
                    for(int i = 0; i < tscales.length; i++){
                        Tmp.v1.trns(b.rot() + 180f, (lenscales[i] - 1f) * 35f);
                        Lines.stroke((9f + Mathf.absin(Time.time(), 0.8f, 1.5f)) * b.fout() * strokes[s] * tscales[i]);
                        Lines.lineAngle(b.x + Tmp.v1.x, b.y + Tmp.v1.y, b.rot(), baseLen * lenscales[i], CapStyle.none);
                    }
                }
                Draw.reset();
            }
        };

        waterShot = new LiquidBulletType(Liquids.water){{
            knockback = 0.7f;
        }};

        cryoShot = new LiquidBulletType(Liquids.cryofluid){{

        }};

        slagShot = new LiquidBulletType(Liquids.slag){{
            damage = 4;
            drag = 0.03f;
        }};

        eruptorShot = new LiquidBulletType(Liquids.slag){{
            damage = 2;
            speed = 2.1f;
            drag = 0.02f;
        }};

        oilShot = new LiquidBulletType(Liquids.oil){{
            drag = 0.03f;
        }};

        lightning = new BulletType(0.001f, 12f){
            {
                lifetime = 1f;
                shootEffect = Fx.hitLancer;
                smokeEffect = Fx.none;
                despawnEffect = Fx.none;
                hitEffect = Fx.hitLancer;
                keepVelocity = false;
            }

            @Override
            public float range(){
                return 70f;
            }

            @Override
            public void draw(Bullet b){
            }

            @Override
            public void init(Bullet b){
                Lightning.create(b.getTeam(), Pal.lancerLaser, damage * (b.getOwner() instanceof Player ? state.rules.playerDamageMultiplier : 1f), b.x, b.y, b.rot(), 30);
            }
        };

        arc = new BulletType(0.001f, 21){
            {
                lifetime = 1;
                despawnEffect = Fx.none;
                hitEffect = Fx.hitLancer;
            }

            @Override
            public void draw(Bullet b){
            }

            @Override
            public void init(Bullet b){
                Lightning.create(b.getTeam(), Pal.lancerLaser, damage, b.x, b.y, b.rot(), 25);
            }
        };

        driverBolt = new MassDriverBolt();

        frag = new BasicBulletType(5f, 8, "bullet"){{
            bulletWidth = 8f;
            bulletHeight = 9f;
            bulletShrink = 0.5f;
            lifetime = 50f;
            drag = 0.04f;
        }};

        bombExplosive = new BombBulletType(10f, 20f, "shell"){{
            bulletWidth = 9f;
            bulletHeight = 13f;
            hitEffect = Fx.flakExplosion;
            shootEffect = Fx.none;
            smokeEffect = Fx.none;
        }};

        bombIncendiary = new BombBulletType(7f, 10f, "shell"){{
            bulletWidth = 8f;
            bulletHeight = 12f;
            hitEffect = Fx.flakExplosion;
            backColor = Pal.lightOrange;
            frontColor = Pal.lightishOrange;
            incendChance = 1f;
            incendAmount = 3;
            incendSpread = 10f;
        }};

        bombOil = new BombBulletType(2f, 3f, "shell"){
            {
                bulletWidth = 8f;
                bulletHeight = 12f;
                hitEffect = Fx.pulverize;
                backColor = new Color(0x4f4f4fff);
                frontColor = Color.gray;
            }

            @Override
            public void hit(Bullet b, float x, float y){
                super.hit(b, x, y);

                for(int i = 0; i < 3; i++){
                    Tile tile = world.tileWorld(x + Mathf.range(8f), y + Mathf.range(8f));
                    Puddle.deposit(tile, Liquids.oil, 5f);
                }
            }
        };
    }
}
