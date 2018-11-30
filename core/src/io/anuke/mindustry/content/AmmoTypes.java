package io.anuke.mindustry.content;

import io.anuke.mindustry.content.bullets.*;
import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.content.fx.ShootFx;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.type.ContentType;

public class AmmoTypes implements ContentList{
    public static AmmoType
        bulletCopper, bulletDense, bulletThorium, bulletSilicon, bulletPyratite,
        bulletDenseBig, bulletPyratiteBig, bulletThoriumBig,
        shock, bombExplosive, bombIncendiary, bombOil, shellCarbide, flamerThermite, weaponMissile, weaponMissileSwarm, bulletMech,
        healBlaster, bulletGlaive,
        flakExplosive, flakPlastic, flakSurge,
        missileExplosive, missileIncindiary, missileSurge,
        artilleryDense, artilleryPlastic, artilleryHoming, artilleryIncindiary, artilleryExplosive, unitArtillery,
        basicFlame, lancerLaser, lightning, meltdownLaser, burstLaser,
            fuseShotgun, oil, water, lava, cryofluid, arc;

    @Override
    public void load(){

        //weapon specific

        bulletMech = new AmmoType(StandardBullets.mechSmall){{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
            reloadMultiplier = 1f;
            inaccuracy = 5f;
        }};

        bulletGlaive = new AmmoType(Items.pyratite, StandardBullets.glaive, 3){{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
            inaccuracy = 3f;
        }};

        healBlaster = new AmmoType(TurretBullets.healBullet){{
            shootEffect = ShootFx.shootHeal;
            smokeEffect = BulletFx.hitLaser;
            reloadMultiplier = 1f;
            inaccuracy = 2f;
        }};

        shock = new AmmoType(TurretBullets.lightning){{
            shootEffect = BulletFx.hitLancer;
            smokeEffect = Fx.none;
        }};

        shellCarbide = new AmmoType(WeaponBullets.shellCarbide){{
            shootEffect = ShootFx.shootBig;
            smokeEffect = ShootFx.shootBigSmoke;
        }};

        bombExplosive = new AmmoType(WeaponBullets.bombExplosive){{
            shootEffect = Fx.none;
            smokeEffect = Fx.none;
        }};

        bombIncendiary = new AmmoType(WeaponBullets.bombIncendiary){{
            shootEffect = Fx.none;
            smokeEffect = Fx.none;
        }};

        bombOil = new AmmoType(WeaponBullets.bombOil){{
            shootEffect = Fx.none;
            smokeEffect = Fx.none;
        }};

        flamerThermite = new AmmoType(TurretBullets.basicFlame){{
            shootEffect = ShootFx.shootSmallFlame;
        }};

        weaponMissile = new AmmoType(MissileBullets.javelin){{
            shootEffect = BulletFx.hitBulletSmall;
            smokeEffect = Fx.none;
            reloadMultiplier = 1.2f;
        }};

        weaponMissileSwarm = new AmmoType(MissileBullets.swarm){{
            shootEffect = BulletFx.hitBulletSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
            reloadMultiplier = 1.2f;
        }};

        //bullets

        bulletCopper = new AmmoType(Items.copper, StandardBullets.copper, 5){{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
            reloadMultiplier = 1f;
            inaccuracy = 5f;
        }};

        bulletDense = new AmmoType(Items.densealloy, StandardBullets.dense, 2){{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
            reloadMultiplier = 0.6f;
        }};

        bulletThorium = new AmmoType(Items.thorium, StandardBullets.thorium, 2){{
            shootEffect = ShootFx.shootBig;
            smokeEffect = ShootFx.shootBigSmoke;
        }};

        bulletSilicon = new AmmoType(Items.silicon, StandardBullets.homing, 5){{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
            reloadMultiplier = 1.4f;
        }};

        bulletPyratite = new AmmoType(Items.pyratite, StandardBullets.tracer, 3){{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
            inaccuracy = 3f;
        }};

        bulletDenseBig = new AmmoType(Items.densealloy, StandardBullets.denseBig, 1){{
            shootEffect = ShootFx.shootBig;
            smokeEffect = ShootFx.shootBigSmoke;
        }};

        bulletThoriumBig = new AmmoType(Items.thorium, StandardBullets.thoriumBig, 1){{
            shootEffect = ShootFx.shootBig;
            smokeEffect = ShootFx.shootBigSmoke;
        }};

        bulletPyratiteBig = new AmmoType(Items.pyratite, StandardBullets.tracerBig, 2){{
            shootEffect = ShootFx.shootBig;
            smokeEffect = ShootFx.shootBigSmoke;
            inaccuracy = 3f;
        }};

        //flak

        flakExplosive = new AmmoType(Items.blastCompound, FlakBullets.explosive, 5){{
            shootEffect = ShootFx.shootBig;
            smokeEffect = ShootFx.shootBigSmoke;
        }};

        flakPlastic = new AmmoType(Items.plastanium, FlakBullets.plastic, 5){{
            shootEffect = ShootFx.shootBig;
            smokeEffect = ShootFx.shootBigSmoke;
        }};

        flakSurge = new AmmoType(Items.surgealloy, FlakBullets.surge, 5){{
            shootEffect = ShootFx.shootBig;
            smokeEffect = ShootFx.shootBigSmoke;
            reloadMultiplier = 1/2f;
        }};

        //missiles

        missileExplosive = new AmmoType(Items.blastCompound, MissileBullets.explosive, 1){{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
            reloadMultiplier = 1.2f;
        }};

        missileIncindiary = new AmmoType(Items.pyratite, MissileBullets.incindiary, 1){{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
            reloadMultiplier = 1.0f;
        }};

        missileSurge = new AmmoType(Items.surgealloy, MissileBullets.surge, 1){{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
            reloadMultiplier = 1.1f;
        }};

        //artillery

        artilleryDense = new AmmoType(Items.densealloy, ArtilleryBullets.dense, 2){{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
        }};

        artilleryPlastic = new AmmoType(Items.plastanium, ArtilleryBullets.plastic, 2){{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
            reloadMultiplier = 1.4f;
        }};

        artilleryHoming = new AmmoType(Items.silicon, ArtilleryBullets.homing, 1){{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
            reloadMultiplier = 0.9f;
        }};

        artilleryIncindiary = new AmmoType(Items.pyratite, ArtilleryBullets.incindiary, 2){{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
            reloadMultiplier = 1.2f;
        }};

        artilleryExplosive = new AmmoType(Items.blastCompound, ArtilleryBullets.explosive, 1){{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
            reloadMultiplier = 1.6f;
        }};

        unitArtillery = new AmmoType(Items.blastCompound, ArtilleryBullets.unit, 1){{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
            reloadMultiplier = 1.6f;
        }};


        //flame

        basicFlame = new AmmoType(Liquids.oil, TurretBullets.basicFlame, 0.3f){{
            shootEffect = ShootFx.shootSmallFlame;
        }};

        //power

        lancerLaser = new AmmoType(TurretBullets.lancerLaser);

        burstLaser = new AmmoType(TurretBullets.burstLaser){{
            range = 60f;
        }};

        lightning = new AmmoType(TurretBullets.lightning);

        arc = new AmmoType(TurretBullets.arc);

        meltdownLaser = new AmmoType(TurretBullets.meltdownLaser);

        fuseShotgun = new AmmoType(Items.densealloy, TurretBullets.fuseShot, 1f){{
            shootEffect = Fx.none;
            smokeEffect = ShootFx.shootBigSmoke2;
        }};

        //liquid

        oil = new AmmoType(Liquids.oil, TurretBullets.oilShot, 0.3f);

        water = new AmmoType(Liquids.water, TurretBullets.waterShot, 0.3f);

        lava = new AmmoType(Liquids.lava, TurretBullets.lavaShot, 0.3f);

        cryofluid = new AmmoType(Liquids.cryofluid, TurretBullets.cryoShot, 0.3f);

    }

    @Override
    public ContentType type(){
        return ContentType.ammo;
    }
}
