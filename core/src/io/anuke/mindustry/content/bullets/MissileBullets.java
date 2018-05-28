package io.anuke.mindustry.content.bullets;

import io.anuke.mindustry.entities.bullet.BasicBulletType;
import io.anuke.mindustry.entities.bullet.BulletType;

public class MissileBullets {
    public static final BulletType

    explosive = new BasicBulletType(3f, 5) {
        {
            bulletWidth = 7f;
            bulletHeight = 9f;
        }
    },

    incindiary = new BasicBulletType(3f, 5) {
        {
            bulletWidth = 7f;
            bulletHeight = 9f;
        }
    },

    surge = new BasicBulletType(3f, 5) {
        {
            bulletWidth = 7f;
            bulletHeight = 9f;
        }
    };
}
