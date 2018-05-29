package io.anuke.mindustry.content.bullets;

import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.bullet.BasicBulletType;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.type.ContentList;

public class ShellBullets implements ContentList {
    public static BulletType lead, leadShard, thorium, thoriumShard, plastic, plasticShard, explosive, explosiveShard, incindiary;

    @Override
    public void load() {

        lead = new BasicBulletType(3f, 0) {
            {
                hiteffect = BulletFx.flakExplosion;
                knockback = 0.8f;
                lifetime = 90f;
                drag = 0.01f;
                bulletWidth = bulletHeight = 9f;
                fragBullet = leadShard;
                bulletSprite = "frag";
                bulletShrink = 0.1f;
            }
        };

        leadShard = new BasicBulletType(3f, 0) {
            {
                drag = 0.1f;
                hiteffect = Fx.none;
                despawneffect = Fx.none;
                hitsize = 4;
                lifetime = 20f;
                bulletWidth = 9f;
                bulletHeight = 11f;
                bulletShrink = 1f;
            }
        };

        thorium = new BasicBulletType(3f, 0) {
            {
                hiteffect = BulletFx.flakExplosion;
                knockback = 0.8f;
                lifetime = 90f;
                drag = 0.01f;
                bulletWidth = bulletHeight = 9f;
                fragBullet = leadShard;
                bulletSprite = "frag";
                bulletShrink = 0.1f;
            }
        };

        thoriumShard = new BasicBulletType(3f, 0) {
            {
                drag = 0.1f;
                hiteffect = Fx.none;
                despawneffect = Fx.none;
                hitsize = 4;
                lifetime = 20f;
                bulletWidth = 9f;
                bulletHeight = 11f;
                bulletShrink = 1f;
            }
        };

        plastic = new BasicBulletType(3f, 0) {
            {
                hiteffect = BulletFx.flakExplosion;
                knockback = 0.8f;
                lifetime = 90f;
                drag = 0.01f;
                bulletWidth = bulletHeight = 9f;
                fragBullet = leadShard;
                bulletSprite = "frag";
                bulletShrink = 0.1f;
            }
        };

        plasticShard = new BasicBulletType(3f, 0) {
            {
                drag = 0.1f;
                hiteffect = Fx.none;
                despawneffect = Fx.none;
                hitsize = 4;
                lifetime = 20f;
                bulletWidth = 9f;
                bulletHeight = 11f;
                bulletShrink = 1f;
            }
        };

        explosive = new BasicBulletType(3f, 0) {
            {
                hiteffect = BulletFx.flakExplosion;
                knockback = 0.8f;
                lifetime = 90f;
                drag = 0.01f;
                bulletWidth = bulletHeight = 9f;
                fragBullet = leadShard;
                bulletSprite = "frag";
                bulletShrink = 0.1f;
            }
        };

        explosiveShard = new BasicBulletType(3f, 0) {
            {
                drag = 0.1f;
                hiteffect = Fx.none;
                despawneffect = Fx.none;
                hitsize = 4;
                lifetime = 20f;
                bulletWidth = 9f;
                bulletHeight = 11f;
                bulletShrink = 1f;
            }
        };

        incindiary = new BasicBulletType(3f, 0) {
            {
                hiteffect = BulletFx.flakExplosion;
                knockback = 0.8f;
                lifetime = 90f;
                drag = 0.01f;
                bulletWidth = bulletHeight = 9f;
                fragBullet = leadShard;
                bulletSprite = "frag";
                bulletShrink = 0.1f;
            }
        };
    }
}
