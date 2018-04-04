package io.anuke.mindustry.content;

import io.anuke.mindustry.content.bullets.TurretBullets;
import io.anuke.mindustry.resource.AmmoType;

public class AmmoTypes {
    public static final AmmoType

    basicIron = new AmmoType(Items.iron, TurretBullets.basicIron, 5, 0.9f),

    basicFlame = new AmmoType(Liquids.oil, TurretBullets.basicFlame, 0.3f, 0.9f);
}
