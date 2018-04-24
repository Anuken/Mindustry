package io.anuke.mindustry.content;

import io.anuke.mindustry.content.bullets.TurretBullets;
import io.anuke.mindustry.content.fx.ShootFx;
import io.anuke.mindustry.resource.AmmoType;

public class AmmoTypes {
    public static final AmmoType

    basicIron = new AmmoType(Items.iron, TurretBullets.basicIron, 5){{
        shootEffect = ShootFx.shootSmall;
        smokeEffect = ShootFx.shootSmallSmoke;
    }},

    basicSteel = new AmmoType(Items.steel, TurretBullets.basicSteel, 5){{
        shootEffect = ShootFx.shootBig;
        smokeEffect = ShootFx.shootBigSmoke;
    }},

    basicFlame = new AmmoType(Liquids.oil, TurretBullets.basicFlame, 0.3f){{
        shootEffect = ShootFx.shootSmallFlame;
    }},

    basicLeadFrag = new AmmoType(Items.lead, TurretBullets.basicLeadFragShell, 1){{
        shootEffect = ShootFx.shootBig2;
        smokeEffect = ShootFx.shootBigSmoke2;
    }},

    lancerLaser = new AmmoType(TurretBullets.lancerLaser),

    lightning = new AmmoType(TurretBullets.lightning),

    oil = new AmmoType(Liquids.oil, TurretBullets.oilShot, 0.3f),

    water = new AmmoType(Liquids.water, TurretBullets.waterShot, 0.3f),

    lava = new AmmoType(Liquids.lava, TurretBullets.lavaShot, 0.3f),

    cryofluid = new AmmoType(Liquids.cryofluid, TurretBullets.cryoShot, 0.3f);
}
