package io.anuke.mindustry.content.bullets;

import io.anuke.mindustry.entities.bullet.BasicBulletType;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.type.ContentList;

public class StandardBullets extends BulletList implements ContentList{
    public static BulletType copper, carbide, thorium, homing, tracer;

    @Override
    public void load(){

        copper = new BasicBulletType(2.5f, 7, "bullet"){
            {
                bulletWidth = 7f;
                bulletHeight = 9f;
            }
        };

        carbide = new BasicBulletType(3.5f, 18, "bullet"){
            {
                bulletWidth = 9f;
                bulletHeight = 12f;
                armorPierce = 0.2f;
            }
        };

        thorium = new BasicBulletType(4f, 29, "bullet"){
            {
                bulletWidth = 10f;
                bulletHeight = 13f;
                armorPierce = 0.5f;
            }
        };

        homing = new BasicBulletType(3f, 9, "bullet"){
            {
                bulletWidth = 7f;
                bulletHeight = 9f;
                homingPower = 5f;
            }
        };

        tracer = new BasicBulletType(3.2f, 11, "bullet"){
            {
                bulletWidth = 10f;
                bulletHeight = 12f;
                frontColor = Palette.lightishOrange;
                backColor = Palette.lightOrange;
                incendSpread = 3f;
                incendAmount = 1;
                incendChance = 0.3f;
            }
        };
    }
}
