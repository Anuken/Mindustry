package io.anuke.mindustry.content.bullets;

import io.anuke.mindustry.entities.bullet.BasicBulletType;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.type.ContentList;

public class FlakBullets extends BulletList implements ContentList {
    public static BulletType lead, plastic, explosive, surge;

    @Override
    public void load() {

        lead = new BasicBulletType(3f, 5, "bullet") {
            {
                bulletWidth = 7f;
                bulletHeight = 9f;
            }
        };

        plastic = new BasicBulletType(3f, 5, "bullet") {
            {
                bulletWidth = 7f;
                bulletHeight = 9f;
            }
        };

        explosive = new BasicBulletType(3f, 5, "bullet") {
            {
                bulletWidth = 7f;
                bulletHeight = 9f;
            }
        };

        surge = new BasicBulletType(3f, 5, "bullet") {
            {
                bulletWidth = 7f;
                bulletHeight = 9f;
            }
        };
    }
}
