package io.anuke.mindustry.content;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.bullets.*;
import io.anuke.mindustry.content.fx.ShootFx;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.type.ContentList;

public class AmmoTypes implements ContentList {
    public static AmmoType bulletIron, bulletLead, bulletSteel, bulletThorium, bulletSilicon, bulletThermite, flakLead, flakExplosive, flakPlastic, flakSurge, shellLead, shellExplosive, shellPlastic, shellThorium, missileExplosive, missileIncindiary, missileSurge, artilleryLead, artilleryThorium, artilleryPlastic, artilleryHoming, artilleryIncindiary, basicFlame, lancerLaser, lightning, spectreLaser, meltdownLaser, fuseShotgun, oil, water, lava, cryofluid;

    @Override
    public void load() {

        //bullets

        bulletIron = new AmmoType(Items.iron, StandardBullets.iron, 5) {{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
        }};

        bulletLead = new AmmoType(Items.lead, StandardBullets.lead, 5) {{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
        }};

        bulletSteel = new AmmoType(Items.steel, StandardBullets.steel, 5) {{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
        }};

        bulletThorium = new AmmoType(Items.thorium, StandardBullets.thorium, 5) {{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
        }};

        bulletSilicon = new AmmoType(Items.silicon, StandardBullets.homing, 5) {{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
        }};

        bulletThermite = new AmmoType(Items.thermite, StandardBullets.tracer, 5) {{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
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

        flakPlastic = new AmmoType(Items.plastic, FlakBullets.plastic, 5) {{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
        }};

        flakSurge = new AmmoType(Items.surgealloy, FlakBullets.surge, 5) {{
            shootEffect = ShootFx.shootSmall;
            smokeEffect = ShootFx.shootSmallSmoke;
        }};

        //shells

        shellLead = new AmmoType(Items.lead, ShellBullets.lead, 1) {{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
        }};

        shellExplosive = new AmmoType(Items.blastCompound, ShellBullets.explosive, 1) {{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
        }};

        shellPlastic = new AmmoType(Items.plastic, ShellBullets.plastic, 1) {{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
        }};

        shellThorium = new AmmoType(Items.thorium, ShellBullets.thorium, 1) {{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
        }};

        //missiles

        missileExplosive = new AmmoType(Items.blastCompound, MissileBullets.explosive, 1) {{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
        }};

        missileIncindiary = new AmmoType(Items.thermite, MissileBullets.incindiary, 1) {{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
        }};

        missileSurge = new AmmoType(Items.surgealloy, MissileBullets.surge, 1) {{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
        }};

        //artillery

        artilleryLead = new AmmoType(Items.lead, ArtilleryBullets.lead, 1) {{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
        }};

        artilleryThorium = new AmmoType(Items.thorium, ArtilleryBullets.thorium, 1) {{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
        }};

        artilleryPlastic = new AmmoType(Items.plastic, ArtilleryBullets.plastic, 1) {{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
        }};

        artilleryHoming = new AmmoType(Items.silicon, ArtilleryBullets.homing, 1) {{
            shootEffect = ShootFx.shootBig2;
            smokeEffect = ShootFx.shootBigSmoke2;
        }};

        artilleryIncindiary = new AmmoType(Items.thermite, ArtilleryBullets.incindiary, 1) {{
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

        fuseShotgun = new AmmoType(Items.iron, TurretBullets.fuseShot, 0.1f);

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
