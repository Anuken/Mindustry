package io.anuke.mindustry.content;

import io.anuke.mindustry.content.bullets.TurretBullets;
import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.resource.Weapon;

public class Weapons {
    public static final Weapon

    blaster = new Weapon("blaster", 12, TurretBullets.basicIron) {
        {
            effect = BulletFx.shootSmall;
            length = 2f;
        }
    };
}
