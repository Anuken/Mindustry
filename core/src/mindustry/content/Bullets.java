package mindustry.content;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class Bullets implements ContentList{
    public static BulletType

    //artillery
    artilleryDense, artilleryPlastic, artilleryPlasticFrag, artilleryHoming, artilleryIncendiary, artilleryExplosive,

    //flak
    flakScrap, flakLead, flakPlastic, flakExplosive, flakSurge, flakGlass, glassFrag,

    //missiles
    missileExplosive, missileIncendiary, missileSurge, missileJavelin, missileSwarm,

    //standard
    standardCopper, standardDense, standardThorium, standardHoming, standardIncendiary, standardMechSmall,
    standardGlaive, standardDenseBig, standardThoriumBig, standardIncendiaryBig,

    //electric
    lancerLaser, meltdownLaser, arc, damageLightning,

    //liquid
    waterShot, cryoShot, slagShot, oilShot,

    //environment, misc.
    fireball, basicFlame, pyraFlame, driverBolt, healBullet, healBulletBig, frag,

    //bombs
    bombExplosive, bombIncendiary, bombOil;

    @Override
    public void load(){

        artilleryDense = new ArtilleryBulletType(3f, 12, "shell"){{
            hitEffect = Fx.flakExplosion;
            knockback = 0.8f;
            lifetime = 50f;
            width = height = 11f;
            collidesTiles = false;
            splashDamageRadius = 25f;
            splashDamage = 33f;
        }};

        artilleryPlasticFrag = new BasicBulletType(2.5f, 10, "bullet"){{
            width = 10f;
            height = 12f;
            shrinkY = 1f;
            lifetime = 15f;
            backColor = Pal.plastaniumBack;
            frontColor = Pal.plastaniumFront;
            despawnEffect = Fx.none;
        }};

        artilleryPlastic = new ArtilleryBulletType(3.4f, 12, "shell"){{
            hitEffect = Fx.plasticExplosion;
            knockback = 1f;
            lifetime = 55f;
            width = height = 13f;
            collidesTiles = false;
            splashDamageRadius = 35f;
            splashDamage = 45f;
            fragBullet = artilleryPlasticFrag;
            fragBullets = 10;
            backColor = Pal.plastaniumBack;
            frontColor = Pal.plastaniumFront;
        }};

        artilleryHoming = new ArtilleryBulletType(3f, 12, "shell"){{
            hitEffect = Fx.flakExplosion;
            knockback = 0.8f;
            lifetime = 45f;
            width = height = 11f;
            collidesTiles = false;
            splashDamageRadius = 25f;
            splashDamage = 33f;
            reloadMultiplier = 1.2f;
            ammoMultiplier = 3f;
            homingPower = 0.08f;
            homingRange = 50f;
        }};

        artilleryIncendiary = new ArtilleryBulletType(3f, 12, "shell"){{
            hitEffect = Fx.blastExplosion;
            knockback = 0.8f;
            lifetime = 60f;
            width = height = 13f;
            collidesTiles = false;
            splashDamageRadius = 25f;
            splashDamage = 35f;
            status = StatusEffects.burning;
            frontColor = Pal.lightishOrange;
            backColor = Pal.lightOrange;
            trailEffect = Fx.incendTrail;
        }};

        artilleryExplosive = new ArtilleryBulletType(2f, 12, "shell"){{
            hitEffect = Fx.blastExplosion;
            knockback = 0.8f;
            lifetime = 70f;
            width = height = 14f;
            collidesTiles = false;
            ammoMultiplier = 4f;
            splashDamageRadius = 45f;
            splashDamage = 50f;
            backColor = Pal.missileYellowBack;
            frontColor = Pal.missileYellow;

            status = StatusEffects.blasted;
            statusDuration = 60f;
        }};

        glassFrag = new BasicBulletType(3f, 5, "bullet"){{
            width = 5f;
            height = 12f;
            shrinkY = 1f;
            lifetime = 20f;
            backColor = Pal.gray;
            frontColor = Color.white;
            despawnEffect = Fx.none;
        }};

        flakLead = new FlakBulletType(4.2f, 3){{
            lifetime = 60f;
            ammoMultiplier = 4f;
            shootEffect = Fx.shootSmall;
            width = 6f;
            height = 8f;
            hitEffect = Fx.flakExplosion;
            splashDamage = 27f;
            splashDamageRadius = 15f;
        }};

        flakScrap = new FlakBulletType(4f, 3){{
            lifetime = 60f;
            ammoMultiplier = 5f;
            shootEffect = Fx.shootSmall;
            reloadMultiplier = 0.5f;
            width = 6f;
            height = 8f;
            hitEffect = Fx.flakExplosion;
            splashDamage = 22f;
            splashDamageRadius = 24f;
        }};

        flakGlass = new FlakBulletType(4f, 3){{
            lifetime = 70f;
            ammoMultiplier = 5f;
            shootEffect = Fx.shootSmall;
            reloadMultiplier = 0.8f;
            width = 6f;
            height = 8f;
            hitEffect = Fx.flakExplosion;
            splashDamage = 20f;
            splashDamageRadius = 20f;
            fragBullet = glassFrag;
            fragBullets = 5;
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
            shootEffect = Fx.shootBig;
            ammoMultiplier = 4f;
            splashDamage = 15f;
            splashDamageRadius = 34f;

            status = StatusEffects.blasted;
            statusDuration = 60f;
        }};

        flakSurge = new FlakBulletType(4.5f, 13){{
            splashDamage = 45f;
            splashDamageRadius = 40f;
            lightning = 2;
            lightningLength = 7;
            shootEffect = Fx.shootBig;
        }};

        missileExplosive = new MissileBulletType(2.7f, 10, "missile"){{
            width = 8f;
            height = 8f;
            shrinkY = 0f;
            drag = -0.01f;
            splashDamageRadius = 30f;
            splashDamage = 30f;
            ammoMultiplier = 4f;
            lifetime = 150f;
            hitEffect = Fx.blastExplosion;
            despawnEffect = Fx.blastExplosion;

            status = StatusEffects.blasted;
            statusDuration = 60f;
        }};

        missileIncendiary = new MissileBulletType(2.9f, 12, "missile"){{
            frontColor = Pal.lightishOrange;
            backColor = Pal.lightOrange;
            width = 7f;
            height = 8f;
            shrinkY = 0f;
            drag = -0.01f;
            homingPower = 0.08f;
            splashDamageRadius = 20f;
            splashDamage = 20f;
            lifetime = 160f;
            hitEffect = Fx.blastExplosion;
            status = StatusEffects.burning;
        }};

        missileSurge = new MissileBulletType(4.4f, 20, "bullet"){{
            width = 8f;
            height = 8f;
            shrinkY = 0f;
            drag = -0.01f;
            splashDamageRadius = 28f;
            splashDamage = 40f;
            lifetime = 150f;
            hitEffect = Fx.blastExplosion;
            despawnEffect = Fx.blastExplosion;
            lightning = 2;
            lightningLength = 14;
        }};

        missileJavelin = new MissileBulletType(5f, 10.5f, "missile"){{
            width = 8f;
            height = 8f;
            shrinkY = 0f;
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
            width = 8f;
            height = 8f;
            shrinkY = 0f;
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

        standardCopper = new BasicBulletType(2.5f, 9, "bullet"){{
            width = 7f;
            height = 9f;
            lifetime = 60f;
            shootEffect = Fx.shootSmall;
            smokeEffect = Fx.shootSmallSmoke;
            ammoMultiplier = 2;
        }};

        standardDense = new BasicBulletType(3.5f, 18, "bullet"){{
            width = 9f;
            height = 12f;
            reloadMultiplier = 0.6f;
            ammoMultiplier = 4;
            lifetime = 60f;
        }};

        standardThorium = new BasicBulletType(4f, 29, "bullet"){{
            width = 10f;
            height = 13f;
            shootEffect = Fx.shootBig;
            smokeEffect = Fx.shootBigSmoke;
            ammoMultiplier = 4;
            lifetime = 60f;
        }};

        standardHoming = new BasicBulletType(3f, 12, "bullet"){{
            width = 7f;
            height = 9f;
            homingPower = 0.08f;
            reloadMultiplier = 1.5f;
            ammoMultiplier = 5;
            lifetime = 60f;
        }};

        standardIncendiary = new BasicBulletType(3.2f, 11, "bullet"){{
            width = 10f;
            height = 12f;
            frontColor = Pal.lightishOrange;
            backColor = Pal.lightOrange;
            status = StatusEffects.burning;
            inaccuracy = 3f;
            lifetime = 60f;
        }};

        standardGlaive = new BasicBulletType(4f, 7.5f, "bullet"){{
            width = 10f;
            height = 12f;
            frontColor = Color.valueOf("feb380");
            backColor = Color.valueOf("ea8878");
            status = StatusEffects.burning;
            lifetime = 60f;
        }};

        standardMechSmall = new BasicBulletType(4f, 9, "bullet"){{
            width = 11f;
            height = 14f;
            lifetime = 40f;
            inaccuracy = 5f;
            despawnEffect = Fx.hitBulletSmall;
        }};

        standardDenseBig = new BasicBulletType(7f, 55, "bullet"){{
            width = 15f;
            height = 21f;
            shootEffect = Fx.shootBig;
        }};

        standardThoriumBig = new BasicBulletType(8f, 80, "bullet"){{
            width = 16f;
            height = 23f;
            shootEffect = Fx.shootBig;
        }};

        standardIncendiaryBig = new BasicBulletType(7f, 60, "bullet"){{
            width = 16f;
            height = 21f;
            frontColor = Pal.lightishOrange;
            backColor = Pal.lightOrange;
            status = StatusEffects.burning;
            shootEffect = Fx.shootBig;
        }};

        damageLightning = new BulletType(0.0001f, 0f){{
            lifetime = Fx.lightning.lifetime;
            hitEffect = Fx.hitLancer;
            despawnEffect = Fx.none;
            status = StatusEffects.shocked;
            statusDuration = 10f;
            hittable = false;
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
                collidesTiles = false;
                collides = false;
                drag = 0.03f;
                hitEffect = despawnEffect = Fx.none;
            }

            @Override
            public void init(Bulletc b){
                b.vel().setLength(0.6f + Mathf.random(2f));
            }

            @Override
            public void draw(Bulletc b){
                Draw.color(Pal.lightFlame, Pal.darkFlame, Color.gray, b.fin());
                Fill.circle(b.x(), b.y(), 3f * b.fout());
                Draw.reset();
            }

            @Override
            public void update(Bulletc b){
                if(Mathf.chance(0.04 * Time.delta())){
                    Tile tile = world.tileWorld(b.x(), b.y());
                    if(tile != null){
                        Fires.create(tile);
                    }
                }

                if(Mathf.chance(0.1 * Time.delta())){
                    Fx.fireballsmoke.at(b.x(), b.y());
                }

                if(Mathf.chance(0.1 * Time.delta())){
                    Fx.ballfire.at(b.x(), b.y());
                }
            }
        };

        basicFlame = new BulletType(3f, 30f){
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
                keepVelocity = false;
                hittable = false;
            }

            @Override
            public float range(){
                return 50f;
            }

            @Override
            public void draw(Bulletc b){
            }
        };

        pyraFlame = new BulletType(3.3f, 45f){
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
                hittable = false;
            }

            @Override
            public void draw(Bulletc b){
            }
        };

        lancerLaser = new LaserBulletType(140){{
            colors = new Color[]{Pal.lancerLaser.cpy().mul(1f, 1f, 1f, 0.4f), Pal.lancerLaser, Color.white};
            hitEffect = Fx.hitLancer;
            despawnEffect = Fx.none;
            hitSize = 4;
            lifetime = 16f;
            drawSize = 400f;
        }};

        meltdownLaser = new ContinuousLaserBulletType(70){{
            length = 220f;
            hitEffect = Fx.hitMeltdown;
            drawSize = 420f;

            incendChance = 0.4f;
            incendSpread = 5f;
            incendAmount = 1;
        }};

        waterShot = new LiquidBulletType(Liquids.water){{
            knockback = 0.7f;
        }};

        cryoShot = new LiquidBulletType(Liquids.cryofluid){{

        }};

        slagShot = new LiquidBulletType(Liquids.slag){{
            damage = 4;
            drag = 0.03f;
        }};

        oilShot = new LiquidBulletType(Liquids.oil){{
            drag = 0.03f;
        }};

        arc = new LightningBulletType(){{
            damage = 21;
            lightningLength = 25;
        }};

        driverBolt = new MassDriverBolt();

        frag = new BasicBulletType(5f, 8, "bullet"){{
            width = 8f;
            height = 9f;
            shrinkY = 0.5f;
            lifetime = 50f;
            drag = 0.04f;
        }};

        bombExplosive = new BombBulletType(18f, 25f, "shell"){{
            width = 10f;
            height = 14f;
            hitEffect = Fx.flakExplosion;
            shootEffect = Fx.none;
            smokeEffect = Fx.none;

            status = StatusEffects.blasted;
            statusDuration = 60f;
        }};

        bombIncendiary = new BombBulletType(7f, 10f, "shell"){{
            width = 8f;
            height = 12f;
            hitEffect = Fx.flakExplosion;
            backColor = Pal.lightOrange;
            frontColor = Pal.lightishOrange;
            incendChance = 1f;
            incendAmount = 3;
            incendSpread = 10f;
        }};

        bombOil = new BombBulletType(2f, 3f, "shell"){
            {
                width = 8f;
                height = 12f;
                hitEffect = Fx.pulverize;
                backColor = new Color(0x4f4f4fff);
                frontColor = Color.gray;
            }

            @Override
            public void hit(Bulletc b, float x, float y){
                super.hit(b, x, y);

                for(int i = 0; i < 3; i++){
                    Tile tile = world.tileWorld(x + Mathf.range(8f), y + Mathf.range(8f));
                    Puddles.deposit(tile, Liquids.oil, 5f);
                }
            }
        };
    }
}
