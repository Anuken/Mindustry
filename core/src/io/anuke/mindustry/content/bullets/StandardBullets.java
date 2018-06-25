package io.anuke.mindustry.content.bullets;

import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.bullet.BasicBulletType;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.type.ContentList;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Angles;

public class StandardBullets extends BulletList implements ContentList {
    public static BulletType tungsten, lead, carbide, thorium, homing, tracer;

    @Override
    public void load() {

        tungsten = new BasicBulletType(3.2f, 10, "bullet") {
            {
                bulletWidth = 9f;
                bulletHeight = 11f;
            }
        };

        lead = new BasicBulletType(2.5f, 5, "bullet") {
            {
                bulletWidth = 7f;
                bulletHeight = 9f;
            }
        };

        carbide = new BasicBulletType(3.5f, 18, "bullet") {
            {
                bulletWidth = 9f;
                bulletHeight = 12f;
                armorPierce = 0.2f;
            }
        };

        thorium = new BasicBulletType(4f, 29, "bullet") {
            {
                bulletWidth = 10f;
                bulletHeight = 13f;
                armorPierce = 0.5f;
            }
        };

        homing = new BasicBulletType(3f, 9, "bullet") {
            float homingPower = 5f;
            {
                bulletWidth = 7f;
                bulletHeight = 9f;
            }

            @Override
            public void update(Bullet b) {
                Unit target = Units.getClosestEnemy(b.getTeam(), b.x, b.y, 40f, unit -> true);
                if(target != null){
                    b.getVelocity().setAngle(Angles.moveToward(b.getVelocity().angle(), b.angleTo(target), homingPower * Timers.delta()));
                }
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
