package io.anuke.mindustry.content;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.bullets.*;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.content.fx.ShootFx;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.type.ContentList;

public class AmmoTypes implements ContentList {
    public static AmmoType bulletTungsten, bulletLead, bulletCarbide, bulletThorium, bulletSilicon, bulletThermite,
            shotgunTungsten, bombExplosive, bombIncendiary, bombOil, shellCarbide, flamerThermite,
            flakLead, flakExplosive, flakPlastic, flakSurge, missileExplosive, missileIncindiary, missileSurge,
            artilleryCarbide, artilleryThorium, artilleryPlastic, artilleryHoming, artilleryIncindiary,
            basicFlame, lancerLaser, lightning, spectreLaser, meltdownLaser, fuseShotgun, oil, water, lava, cryofluid;

    @Override
    public void load() {

        //weapon specific

        shotgunTungsten = new AmmoType(Items.tungsten, WeaponBullets.tungstenShotgun, 2) {{
            shootEffect = ShootFx.shootBig;
            smokeEffect = ShootFx.shootBigSmoke;
            recoil = 1f;
        }};

        shellCarbide = new AmmoType(Items.carbide, WeaponBullets.shellCarbide, 2) {{
            shootEffect = ShootFx.shootBig;
            smokeEffect = ShootFx.shootBigSmoke;
        }};

        bombExplosive = new AmmoType(Items.blastCompound, WeaponBullets.bombExplosive, 3) {{
            shootEffect = Fx.none;
            smokeEffect = Fx.none;
        }};

        bombIncendiary = new AmmoType(Items.pyratite, WeaponBullets.bombIncendiary, 3) {{
            shootEffect = Fx.none;
            smokeEffect = Fx.none;
        }};

        bombOil = new AmmoType(Items.coal, WeaponBullets.bombOil, 3) {{
            shootEffect = Fx.none;
            smokeEffect = Fx.none;
        }};

        flamerThermite = new AmmoType(Items.pyratite, TurretBullets.basicFlame, 3) {{
            shootEffect = ShootFx.shootSmallFlame;
        }};

        //bullets

        bulletLead = new AmmoType(Items.lead, StandardBullets.lead, 5) {{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
            reloadMultiplier = 1.6f;
            inaccuracy = 5f;
        }};

        bulletTungsten = new AmmoType(Items.tungsten, StandardBullets.tungsten, 2) {{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
            reloadMultiplier = 0.8f;
        }};

        bulletCarbide = new AmmoType(Items.carbide, StandardBullets.carbide, 2) {{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
            reloadMultiplier = 0.6f;
        }};

        bulletThorium = new AmmoType(Items.thorium, StandardBullets.thorium, 2) {{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
        }};

        bulletSilicon = new AmmoType(Items.silicon, StandardBullets.homing, 5) {{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
            reloadMultiplier = 1.4f;
        }};

        bulletThermite = new AmmoType(Items.pyratite, StandardBullets.tracer, 3) {{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
            inaccuracy = 3f;
        }};

        //flak

        flakLead = new AmmoType(Items.lead, FlakBullets.lead, 5) {{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
        }};

        flakExplosive = new AmmoType(Items.blastCompound, FlakBullets.explosive, 5) {{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
        }};

        flakPlastic = new AmmoType(Items.plastanium, FlakBullets.plastic, 5) {{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
        }};

        flakSurge = new AmmoType(Items.surgealloy, FlakBullets.surge, 5) {{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
        }};

        //missiles

        missileExplosive = new AmmoType(Items.blastCompound, MissileBullets.explosive, 1) {{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
        }};

        missileIncindiary = new AmmoType(Items.pyratite, MissileBullets.incindiary, 1) {{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
        }};

        missileSurge = new AmmoType(Items.surgealloy, MissileBullets.surge, 1) {{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
        }};

        //artillery

        artilleryCarbide = new AmmoType(Items.carbide, ArtilleryBullets.carbide, 1) {{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
        }};

        artilleryThorium = new AmmoType(Items.thorium, ArtilleryBullets.thorium, 1) {{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
        }};

        artilleryPlastic = new AmmoType(Items.plastanium, ArtilleryBullets.plastic, 1) {{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
        }};

        artilleryHoming = new AmmoType(Items.silicon, ArtilleryBullets.homing, 1) {{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
        }};

        artilleryIncindiary = new AmmoType(Items.pyratite, ArtilleryBullets.incindiary, 1) {{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
        }};

        //flame

        basicFlame = new AmmoType(Liquids.oil, TurretBullets.basicFlame, 0.3f) {{
            shootEffect = ShootFx.shootSmallFlame;
        }};

        //power

        lancerLaser = new AmmoType(TurretBullets.lancerLaser);

        lightning = new AmmoType(TurretBullets.lightning);

        spectreLaser = new AmmoType(TurretBullets.lancerLaser);

        meltdownLaser = new AmmoType(TurretBullets.lancerLaser);

        fuseShotgun = new AmmoType(Items.tungsten, TurretBullets.fuseShot, 0.1f);

        //liquid

        oil = new AmmoType(Liquids.oil, TurretBullets.oilShot, 0.3f);

        water = new AmmoType(Liquids.water, TurretBullets.waterShot, 0.3f);

        lava = new AmmoType(Liquids.lava, TurretBullets.lavaShot, 0.3f);

        cryofluid = new AmmoType(Liquids.cryofluid, TurretBullets.cryoShot, 0.3f);

    }

    @Override
    public Array<? extends Content> getAll() {
        return AmmoType.all();
    }
}
