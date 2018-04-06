package io.anuke.mindustry.content;

import io.anuke.mindustry.content.bullets.TurretBullets;
import io.anuke.mindustry.resource.AmmoType;

public class AmmoTypes {
    public static final AmmoType

    basicIron = new AmmoType(Items.iron, TurretBullets.basicIron, 5, 0.9f),

    basicSteel = new AmmoType(Items.steel, TurretBullets.basicSteel, 5, 0.8f),

    basicFlame = new AmmoType(Liquids.oil, TurretBullets.basicFlame, 0.3f, 0.9f),

    basicLeadFrag = new AmmoType(Items.lead, TurretBullets.basicLeadFragShell, 1, 0.8f),

    lancerLaser = new AmmoType(TurretBullets.lancerLaser),

    oil = new AmmoType(Liquids.oil, TurretBullets.oilShot, 0.3f, 1f),

    water = new AmmoType(Liquids.water, TurretBullets.waterShot, 0.3f, 1f),

    lava = new AmmoType(Liquids.lava, TurretBullets.lavaShot, 0.3f, 1f),

    cryofluid = new AmmoType(Liquids.cryofluid, TurretBullets.cryoShot, 0.3f, 1f);
}
