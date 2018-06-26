package io.anuke.mindustry.content.bullets;

import io.anuke.mindustry.entities.bullet.BasicBulletType;
import io.anuke.mindustry.entities.bullet.BulletType;

public class WeaponBullets extends BulletList {
    public static BulletType tungstenShotgun;

    @Override
    public void load() {
        tungstenShotgun = new BasicBulletType(5f, 8, "bullet") {
            {
                bulletWidth = 8f;
                bulletHeight = 9f;
                bulletShrink = 0.6f;
                lifetime = 30f;
                drag = 0.04f;
            }
        };
    }
}
