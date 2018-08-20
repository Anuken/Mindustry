package io.anuke.mindustry.content;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.bullets.*;
import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.content.fx.ShootFx;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.type.ContentList;

public class AmmoTypes implements ContentList{
    public static AmmoType bulletCopper, bulletCarbide, bulletThorium, bulletSilicon, bulletPyratite,
            shotgunTungsten, bombExplosive, bombIncendiary, bombOil, shellCarbide, flamerThermite, weaponMissile,
            flakLead, flakExplosive, flakPlastic, flakSurge, missileExplosive, missileIncindiary, missileSurge,
            artilleryCarbide, artilleryPlastic, artilleryHoming, artilleryIncindiary, artilleryExplosive,
            basicFlame, lancerLaser, lightning, spectreLaser, meltdownLaser, fuseShotgun, oil, water, lava, cryofluid;

    @Override
    public void load(){

        //weapon specific

        shotgunTungsten = new AmmoType(WeaponBullets.tungstenShotgun){{
            shootEffect = ShootFx.shootBig;
            smokeEffect = ShootFx.shootBigSmoke;
            recoil = 1f;
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

        //bullets

        bulletCopper = new AmmoType(Items.copper, StandardBullets.copper, 5){{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
            reloadMultiplier = 1f;
            inaccuracy = 5f;
        }};

        bulletCarbide = new AmmoType(Items.carbide, StandardBullets.carbide, 2){{
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

        //flak

        flakLead = new AmmoType(Items.lead, FlakBullets.lead, 5){{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
        }};

        flakExplosive = new AmmoType(Items.blastCompound, FlakBullets.explosive, 5){{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
        }};

        flakPlastic = new AmmoType(Items.plastanium, FlakBullets.plastic, 5){{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
        }};

        flakSurge = new AmmoType(Items.surgealloy, FlakBullets.surge, 5){{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
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
        }};

        //artillery

        artilleryCarbide = new AmmoType(Items.carbide, ArtilleryBullets.carbide, 2){{
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

        //flame

        basicFlame = new AmmoType(Liquids.oil, TurretBullets.basicFlame, 0.3f){{
            shootEffect = ShootFx.shootSmallFlame;
        }};

        //power

        lancerLaser = new AmmoType(TurretBullets.lancerLaser);

        lightning = new AmmoType(TurretBullets.lightning);

        spectreLaser = new AmmoType(TurretBullets.lancerLaser);

        meltdownLaser = new AmmoType(TurretBullets.lancerLaser);

        fuseShotgun = new AmmoType(Items.copper, TurretBullets.fuseShot, 0.1f);

        //liquid

        oil = new AmmoType(Liquids.oil, TurretBullets.oilShot, 0.3f);

        water = new AmmoType(Liquids.water, TurretBullets.waterShot, 0.3f);

        lava = new AmmoType(Liquids.lava, TurretBullets.lavaShot, 0.3f);

        cryofluid = new AmmoType(Liquids.cryofluid, TurretBullets.cryoShot, 0.3f);

    }

    @Override
    public Array<? extends Content> getAll(){
        return AmmoType.all();
    }
}
