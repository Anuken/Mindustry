package io.anuke.mindustry.content.bullets;

import io.anuke.mindustry.entities.bullet.BasicBulletType;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.type.ContentList;

public class MissileBullets implements ContentList {
    public static BulletType explosive, incindiary, surge;

    @Override
    public void load() {

        explosive = new BasicBulletType(3f, 5) {
            {
                bulletWidth = 7f;
                bulletHeight = 9f;
            }
        };

        incindiary = new BasicBulletType(3f, 5) {
            {
                bulletWidth = 7f;
                bulletHeight = 9f;
            }
        };

        surge = new BasicBulletType(3f, 5) {
            {
                bulletWidth = 7f;
                bulletHeight = 9f;
            }
        };
    }
}
