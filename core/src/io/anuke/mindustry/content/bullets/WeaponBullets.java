package io.anuke.mindustry.content.bullets;

import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;

public class WeaponBullets {
    public static final BulletType

    none = new BulletType(0f, 0) {
        @Override
        public void draw(Bullet b) {

        }
    };
}
