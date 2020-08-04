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
import mindustry.io.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class Bullets implements ContentList{
    public static BulletType

    //artillery
    artilleryDense, artilleryPlastic, artilleryPlasticFrag, artilleryHoming, artilleryIncendiary, artilleryExplosive,

    //flak
    flakScrap, flakLead, flakGlass, flakGlassFrag,

    //frag (flak-like but hits ground)
    fragGlass, fragExplosive, fragPlastic, fragSurge, fragGlassFrag, fragPlasticFrag,

    //missiles
    missileExplosive, missileIncendiary, missileSurge,

    //standard
    standardCopper, standardDense, standardThorium, standardHoming, standardIncendiary, standardMechSmall,
    standardGlaive, standardDenseBig, standardThoriumBig, standardIncendiaryBig,

    //liquid
    waterShot, cryoShot, slagShot, oilShot,

    //environment, misc.
    damageLightning, damageLightningGround, fireball, basicFlame, pyraFlame, driverBolt, healBullet, healBulletBig, frag;

    @Override
    public void load(){

        artilleryDense = new ArtilleryBulletType(3f, 12, "shell"){{
            hitEffect = Fx.flakExplosion;
            knockback = 0.8f;
            lifetime = 80f;
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
            collidesAir = false;
        }};

        artilleryPlastic = new ArtilleryBulletType(3.4f, 12, "shell"){{
            hitEffect = Fx.plasticExplosion;
            knockback = 1f;
            lifetime = 80f;
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
            lifetime = 80f;
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
            lifetime = 80f;
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
            lifetime = 80f;
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

        flakGlassFrag = new BasicBulletType(3f, 5, "bullet"){{
            width = 5f;
            height = 12f;
            shrinkY = 1f;
            lifetime = 20f;
            backColor = Pal.gray;
            frontColor = Color.white;
            despawnEffect = Fx.none;
            collidesGround = false;
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
            fragBullet = flakGlassFrag;
            fragBullets = 5;
        }};

        fragGlassFrag = new BasicBulletType(3f, 5, "bullet"){{
            width = 5f;
            height = 12f;
            shrinkY = 1f;
            lifetime = 20f;
            backColor = Pal.gray;
            frontColor = Color.white;
            despawnEffect = Fx.none;
        }};

        fragPlasticFrag = new BasicBulletType(2.5f, 10, "bullet"){{
            width = 10f;
            height = 12f;
            shrinkY = 1f;
            lifetime = 15f;
            backColor = Pal.plastaniumBack;
            frontColor = Pal.plastaniumFront;
            despawnEffect = Fx.none;
        }};

        fragGlass = new FlakBulletType(4f, 3){{
            lifetime = 70f;
            ammoMultiplier = 5f;
            shootEffect = Fx.shootSmall;
            reloadMultiplier = 0.8f;
            width = 6f;
            height = 8f;
            hitEffect = Fx.flakExplosion;
            splashDamage = 18f;
            splashDamageRadius = 16f;
            fragBullet = flakGlassFrag;
            fragBullets = 3;
            explodeRange = 20f;
            collidesGround = true;
        }};

        fragPlastic = new FlakBulletType(4f, 6){{
            splashDamageRadius = 40f;
            splashDamage = 25f;
            fragBullet = fragPlasticFrag;
            fragBullets = 5;
            hitEffect = Fx.plasticExplosion;
            frontColor = Pal.plastaniumFront;
            backColor = Pal.plastaniumBack;
            shootEffect = Fx.shootBig;
            collidesGround = true;
            explodeRange = 20f;
        }};

        fragExplosive = new FlakBulletType(4f, 5){{
            shootEffect = Fx.shootBig;
            ammoMultiplier = 4f;
            splashDamage = 15f;
            splashDamageRadius = 34f;
            collidesGround = true;

            status = StatusEffects.blasted;
            statusDuration = 60f;
        }};

        fragSurge = new FlakBulletType(4.5f, 13){{
            splashDamage = 45f;
            splashDamageRadius = 40f;
            lightning = 2;
            lightningLength = 7;
            shootEffect = Fx.shootBig;
            collidesGround = true;
            explodeRange = 20f;
        }};

        missileExplosive = new MissileBulletType(2.7f, 10){{
            width = 8f;
            height = 8f;
            shrinkY = 0f;
            drag = -0.01f;
            splashDamageRadius = 30f;
            splashDamage = 30f;
            ammoMultiplier = 4f;
            lifetime = 100f;
            hitEffect = Fx.blastExplosion;
            despawnEffect = Fx.blastExplosion;

            status = StatusEffects.blasted;
            statusDuration = 60f;
        }};

        missileIncendiary = new MissileBulletType(2.9f, 12){{
            frontColor = Pal.lightishOrange;
            backColor = Pal.lightOrange;
            width = 7f;
            height = 8f;
            shrinkY = 0f;
            drag = -0.01f;
            homingPower = 0.08f;
            splashDamageRadius = 20f;
            splashDamage = 20f;
            lifetime = 100f;
            hitEffect = Fx.blastExplosion;
            status = StatusEffects.burning;
        }};

        missileSurge = new MissileBulletType(4.4f, 20){{
            width = 8f;
            height = 8f;
            shrinkY = 0f;
            drag = -0.01f;
            splashDamageRadius = 28f;
            splashDamage = 40f;
            lifetime = 100f;
            hitEffect = Fx.blastExplosion;
            despawnEffect = Fx.blastExplosion;
            lightning = 2;
            lightningLength = 14;
        }};

        standardCopper = new BasicBulletType(2.5f, 9){{
            width = 7f;
            height = 9f;
            lifetime = 60f;
            shootEffect = Fx.shootSmall;
            smokeEffect = Fx.shootSmallSmoke;
            ammoMultiplier = 2;
        }};

        standardDense = new BasicBulletType(3.5f, 18){{
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

        //this is just a copy of the damage lightning bullet that doesn't damage air units
        damageLightningGround = new BulletType(0.0001f, 0f){{
            collidesAir = false;
        }};
        JsonIO.copy(damageLightning, damageLightningGround);

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
            public void init(Bullet b){
                b.vel.setLength(0.6f + Mathf.random(2f));
            }

            @Override
            public void draw(Bullet b){
                Draw.color(Pal.lightFlame, Pal.darkFlame, Color.gray, b.fin());
                Fill.circle(b.x, b.y, 3f * b.fout());
                Draw.reset();
            }

            @Override
            public void update(Bullet b){
                if(Mathf.chance(0.04 * Time.delta)){
                    Tile tile = world.tileWorld(b.x, b.y);
                    if(tile != null){
                        Fires.create(tile);
                    }
                }

                if(Mathf.chance(0.1 * Time.delta)){
                    Fx.fireballsmoke.at(b.x, b.y);
                }

                if(Mathf.chance(0.1 * Time.delta)){
                    Fx.ballfire.at(b.x, b.y);
                }
            }
        };

        basicFlame = new BulletType(3.35f, 15f){{
            ammoMultiplier = 3f;
            hitSize = 7f;
            lifetime = 18f;
            pierce = true;
            statusDuration = 60f * 4;
            shootEffect = Fx.shootSmallFlame;
            hitEffect = Fx.hitFlameSmall;
            despawnEffect = Fx.none;
            status = StatusEffects.burning;
            keepVelocity = false;
            hittable = false;
        }};

        pyraFlame = new BulletType(3.35f, 22f){{
            ammoMultiplier = 4f;
            hitSize = 7f;
            lifetime = 18f;
            pierce = true;
            statusDuration = 60f * 6;
            shootEffect = Fx.shootPyraFlame;
            hitEffect = Fx.hitFlameSmall;
            despawnEffect = Fx.none;
            status = StatusEffects.burning;
            hittable = false;
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

        driverBolt = new MassDriverBolt();

        frag = new BasicBulletType(5f, 8, "bullet"){{
            width = 8f;
            height = 9f;
            shrinkY = 0.5f;
            lifetime = 50f;
            drag = 0.04f;
        }};
    }
}
