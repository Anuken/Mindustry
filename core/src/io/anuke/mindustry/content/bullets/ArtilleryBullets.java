package io.anuke.mindustry.content.bullets;

import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.entities.bullet.ArtilleryBulletType;
import io.anuke.mindustry.entities.bullet.BasicBulletType;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.type.ContentList;

public class ArtilleryBullets extends BulletList implements ContentList{
    public static BulletType carbide, thorium, plastic, homing, incindiary, surge;

    @Override
    public void load() {

        carbide = new ArtilleryBulletType(3f, 4, "shell") {
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

        thorium = new BasicBulletType(3f, 0, "shell") {
            {
                hiteffect = BulletFx.flakExplosion;
                knockback = 0.8f;
                lifetime = 90f;
                drag = 0.01f;
                bulletWidth = bulletHeight = 9f;
                bulletShrink = 0.1f;
            }
        };

        plastic = new BasicBulletType(3f, 0, "shell") {
            {
                hiteffect = BulletFx.flakExplosion;
                knockback = 0.8f;
                lifetime = 90f;
                drag = 0.01f;
                bulletWidth = bulletHeight = 9f;
                bulletShrink = 0.1f;
            }
        };

        homing = new BasicBulletType(3f, 0, "shell") {
            {
                hiteffect = BulletFx.flakExplosion;
                knockback = 0.8f;
                lifetime = 90f;
                drag = 0.01f;
                bulletWidth = bulletHeight = 9f;
                bulletShrink = 0.1f;
            }
        };

        incindiary = new BasicBulletType(3f, 0, "shell") {
            {
                hiteffect = BulletFx.flakExplosion;
                knockback = 0.8f;
                lifetime = 90f;
                drag = 0.01f;
                bulletWidth = bulletHeight = 9f;
                bulletShrink = 0.1f;
            }
        };

        surge = new BasicBulletType(3f, 0, "shell") {
            {
                hiteffect = BulletFx.flakExplosion;
                knockback = 0.8f;
                lifetime = 90f;
                drag = 0.01f;
                bulletWidth = bulletHeight = 9f;
                bulletShrink = 0.1f;
            }
        };
    }
}
