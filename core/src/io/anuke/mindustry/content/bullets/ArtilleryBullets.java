package io.anuke.mindustry.content.bullets;

import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.bullet.ArtilleryBulletType;
import io.anuke.mindustry.entities.bullet.BasicBulletType;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.game.ContentList;

public class ArtilleryBullets extends BulletList implements ContentList{
    public static BulletType dense, plastic, plasticFrag, homing, incindiary, explosive, surge, unit;

    @Override
    public void load(){

        dense = new ArtilleryBulletType(3f, 0, "shell"){
            {
                hiteffect = BulletFx.flakExplosion;
                knockback = 0.8f;
                lifetime = 50f;
                bulletWidth = bulletHeight = 11f;
                collidesTiles = false;
                splashDamageRadius = 25f;
                splashDamage = 33f;
            }
        };

        plasticFrag = new BasicBulletType(2.5f, 6, "bullet"){
            {
                bulletWidth = 10f;
                bulletHeight = 12f;
                bulletShrink = 1f;
                lifetime = 15f;
                backColor = Palette.plastaniumBack;
                frontColor = Palette.plastaniumFront;
                despawneffect = Fx.none;
            }
        };

        plastic = new ArtilleryBulletType(3.3f, 0, "shell"){
            {
                hiteffect = BulletFx.plasticExplosion;
                knockback = 1f;
                lifetime = 55f;
                bulletWidth = bulletHeight = 13f;
                collidesTiles = false;
                splashDamageRadius = 35f;
                splashDamage = 35f;
                fragBullet = plasticFrag;
                fragBullets = 9;
                backColor = Palette.plastaniumBack;
                frontColor = Palette.plastaniumFront;
            }
        };

        homing = new ArtilleryBulletType(3f, 0, "shell"){
            {
                hiteffect = BulletFx.flakExplosion;
                knockback = 0.8f;
                lifetime = 45f;
                bulletWidth = bulletHeight = 11f;
                collidesTiles = false;
                splashDamageRadius = 25f;
                splashDamage = 33f;
                homingPower = 2f;
                homingRange = 50f;
            }
        };

        incindiary = new ArtilleryBulletType(3f, 0, "shell"){
            {
                hiteffect = BulletFx.blastExplosion;
                knockback = 0.8f;
                lifetime = 60f;
                bulletWidth = bulletHeight = 13f;
                collidesTiles = false;
                splashDamageRadius = 25f;
                splashDamage = 30f;
                incendAmount = 4;
                incendSpread = 11f;
                frontColor = Palette.lightishOrange;
                backColor = Palette.lightOrange;
                trailEffect = BulletFx.incendTrail;
            }
        };

        explosive = new ArtilleryBulletType(2f, 0, "shell"){
            {
                hiteffect = BulletFx.blastExplosion;
                knockback = 0.8f;
                lifetime = 70f;
                bulletWidth = bulletHeight = 14f;
                collidesTiles = false;
                splashDamageRadius = 45f;
                splashDamage = 50f;
                backColor = Palette.missileYellowBack;
                frontColor = Palette.missileYellow;
            }
        };

        unit = new ArtilleryBulletType(2f, 0, "shell"){
            {
                hiteffect = BulletFx.blastExplosion;
                knockback = 0.8f;
                lifetime = 90f;
                bulletWidth = bulletHeight = 14f;
                collides = true;
                collidesTiles = true;
                splashDamageRadius = 45f;
                splashDamage = 50f;
                backColor = Palette.bulletYellowBack;
                frontColor = Palette.bulletYellow;
            }
        };

        surge = new ArtilleryBulletType(3f, 0, "shell"){
            {
                //TODO
            }
        };
    }
}
