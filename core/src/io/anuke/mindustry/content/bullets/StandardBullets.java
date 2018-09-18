package io.anuke.mindustry.content.bullets;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.entities.bullet.BasicBulletType;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.game.ContentList;

public class StandardBullets extends BulletList implements ContentList{
    public static BulletType copper, dense, thorium, homing, tracer, mechSmall, glaive, denseBig, thoriumBig, tracerBig;

    @Override
    public void load(){

        copper = new BasicBulletType(2.5f, 7, "bullet"){
            {
                bulletWidth = 7f;
                bulletHeight = 9f;
            }
        };

        dense = new BasicBulletType(3.5f, 18, "bullet"){
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

        glaive = new BasicBulletType(4f, 7.5f, "bullet"){
            {
                bulletWidth = 10f;
                bulletHeight = 12f;
                frontColor = Color.valueOf("feb380");
                backColor = Color.valueOf("ea8878");
                incendSpread = 3f;
                incendAmount = 1;
                incendChance = 0.3f;
            }
        };

        mechSmall = new BasicBulletType(4f, 9, "bullet"){
            {
                bulletWidth = 11f;
                bulletHeight = 14f;
                lifetime = 40f;
                despawneffect = BulletFx.hitBulletSmall;
            }
        };

        denseBig = new BasicBulletType(7f, 42, "bullet"){
            {
                bulletWidth = 15f;
                bulletHeight = 21f;
                armorPierce = 0.2f;
            }
        };

        thoriumBig = new BasicBulletType(8f, 65, "bullet"){
            {
                bulletWidth = 16f;
                bulletHeight = 23f;
                armorPierce = 0.5f;
            }
        };

        tracerBig = new BasicBulletType(7f, 38, "bullet"){
            {
                bulletWidth = 16f;
                bulletHeight = 21f;
                frontColor = Palette.lightishOrange;
                backColor = Palette.lightOrange;
                incendSpread = 3f;
                incendAmount = 2;
                incendChance = 0.3f;
            }
        };
    }
}
