package io.anuke.mindustry.content.bullets;

import io.anuke.mindustry.entities.bullet.BasicBulletType;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.type.ContentList;

public class StandardBullets extends BulletList implements ContentList {
    public static BulletType iron, lead, steel, thorium, homing, tracer;

    @Override
    public void load() {

        iron = new BasicBulletType(3f, 5, "bullet") {
            {
                bulletWidth = 7f;
                bulletHeight = 9f;
            }
        };

        lead = new BasicBulletType(3f, 5, "bullet") {
            {
                bulletWidth = 7f;
                bulletHeight = 9f;
            }
        };

        steel = new BasicBulletType(3f, 5, "bullet") {
            {
                bulletWidth = 7f;
                bulletHeight = 9f;
            }
        };

        thorium = new BasicBulletType(3f, 5, "bullet") {
            {
                bulletWidth = 7f;
                bulletHeight = 9f;
            }
        };

        homing = new BasicBulletType(3f, 5, "bullet") {
            {
                bulletWidth = 7f;
                bulletHeight = 9f;
            }
        };

        tracer = new BasicBulletType(3f, 5, "bullet") {
            {
                bulletWidth = 7f;
                bulletHeight = 9f;
            }
        };
    }
}
