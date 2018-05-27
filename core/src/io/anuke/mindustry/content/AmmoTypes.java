package io.anuke.mindustry.content;

import io.anuke.mindustry.content.bullets.ShellBullets;
import io.anuke.mindustry.content.bullets.StandardBullets;
import io.anuke.mindustry.content.bullets.TurretBullets;
import io.anuke.mindustry.content.fx.ShootFx;
import io.anuke.mindustry.type.AmmoType;

public class AmmoTypes {
    //TODO add definitions for all ammo types
    public static final AmmoType

    basicIron = new AmmoType(Items.iron, StandardBullets.basicIron, 5){{
        shootEffect = ShootFx.shootSmall;
        smokeEffect = ShootFx.shootSmallSmoke;
    }},

    basicLead = new AmmoType(Items.lead, StandardBullets.basicLead, 5){{
        shootEffect = ShootFx.shootSmall;
        smokeEffect = ShootFx.shootSmallSmoke;
    }},

    standardLead = new AmmoType(AmmoItems.leadBullet, StandardBullets.standardLead, 5){{
        shootEffect = ShootFx.shootSmall;
        smokeEffect = ShootFx.shootSmallSmoke;
    }},

    standardArmorPiercing = new AmmoType(AmmoItems.armorPiercingBullet, StandardBullets.standardArmorPiercing, 5){{
        shootEffect = ShootFx.shootBig;
        smokeEffect = ShootFx.shootBigSmoke;
    }},

    standardHoming = new AmmoType(AmmoItems.homingBullet, StandardBullets.standardHoming, 5){{
        shootEffect = ShootFx.shootBig;
        smokeEffect = ShootFx.shootBigSmoke;
    }},

    standardTracer = new AmmoType(AmmoItems.tracerBullet, StandardBullets.standardTracer, 5){{
        shootEffect = ShootFx.shootBig;
        smokeEffect = ShootFx.shootBigSmoke;
    }},

    basicLeadFlak = new AmmoType(Items.lead, StandardBullets.basicLeadFlak, 5){{
        shootEffect = ShootFx.shootSmall;
        smokeEffect = ShootFx.shootSmallSmoke;
    }},

    standardLeadFlak = new AmmoType(AmmoItems.leadBullet, StandardBullets.standardLeadFlak, 5){{
        shootEffect = ShootFx.shootSmall;
        smokeEffect = ShootFx.shootSmallSmoke;
    }},

    compositeFlak = new AmmoType(AmmoItems.compositeFlak, StandardBullets.compositeFlak, 5){{
        shootEffect = ShootFx.shootSmall;
        smokeEffect = ShootFx.shootSmallSmoke;
    }},

    basicFlame = new AmmoType(Liquids.oil, TurretBullets.basicFlame, 0.3f){{
        shootEffect = ShootFx.shootSmallFlame;
    }},

    basicLeadShell = new AmmoType(Items.lead, ShellBullets.basicLeadShell, 1){{
        shootEffect = ShootFx.shootBig2;
        smokeEffect = ShootFx.shootBigSmoke2;
    }},

    explosiveFragShell = new AmmoType(AmmoItems.explosiveShell, ShellBullets.explosiveShell, 1){{
        shootEffect = ShootFx.shootBig2;
        smokeEffect = ShootFx.shootBigSmoke2;
    }},

    fragShell = new AmmoType(AmmoItems.fragShell, ShellBullets.fragShell, 1){{
        shootEffect = ShootFx.shootBig2;
        smokeEffect = ShootFx.shootBigSmoke2;
    }},

    standardThoriumShell = new AmmoType(AmmoItems.thoriumShell, ShellBullets.thoriumShell, 1){{
        shootEffect = ShootFx.shootBig2;
        smokeEffect = ShootFx.shootBigSmoke2;
    }},

    swarmMissile = new AmmoType(AmmoItems.swarmMissile, ShellBullets.swarmMissile, 1){{
        shootEffect = ShootFx.shootBig2;
        smokeEffect = ShootFx.shootBigSmoke2;
    }},

    scytheMissile = new AmmoType(AmmoItems.scytheMissile, ShellBullets.scytheMissile, 1){{
        shootEffect = ShootFx.shootBig2;
        smokeEffect = ShootFx.shootBigSmoke2;
    }},

    incendiaryMortar = new AmmoType(AmmoItems.incendiaryMortarShell, ShellBullets.incendiaryMortar, 1){{
        shootEffect = ShootFx.shootBig2;
        smokeEffect = ShootFx.shootBigSmoke2;
    }},

    surgeMortar = new AmmoType(AmmoItems.surgeMortarShell, ShellBullets.surgeMortar, 1){{
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
