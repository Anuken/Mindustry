package io.anuke.mindustry.content.bullets;

import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.bullet.MissileBulletType;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.type.ContentList;

public class MissileBullets extends BulletList implements ContentList {
    public static BulletType explosive, incindiary, surge;

    @Override
    public void load() {

        explosive = new MissileBulletType(1.8f, 10, "missile") {
            {
                bulletWidth = 8f;
                bulletHeight = 8f;
                bulletShrink = 0f;
                drag = -0.01f;
                splashDamageRadius = 30f;
                splashDamage = 30f;
                lifetime = 150f;
                hiteffect = BulletFx.blastExplosion;
                despawneffect = BulletFx.blastExplosion;
            }
        };

        incindiary = new MissileBulletType(2f, 12, "missile") {
            {
                frontColor = Palette.lightishOrange;
                backColor = Palette.lightOrange;
                bulletWidth = 7f;
                bulletHeight = 8f;
                bulletShrink = 0f;
                drag = -0.01f;
                homingPower = 7f;
                splashDamageRadius = 10f;
                splashDamage = 10f;
                lifetime = 160f;
                hiteffect = BulletFx.blastExplosion;
                incendSpread = 10f;
                incendAmount = 3;
            }
        };

        surge = new MissileBulletType(3f, 5, "bullet") {
            {
                bulletWidth = 7f;
                bulletHeight = 9f;
            }
        };
    }
}
